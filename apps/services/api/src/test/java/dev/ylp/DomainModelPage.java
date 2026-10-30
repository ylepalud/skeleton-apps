package dev.ylp;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaPackage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.SpringBean;
import org.springframework.stereotype.Component;

/**
 * The domain model page of the documentation site, in Markdown with Mermaid diagrams. Spring
 * Modulith gives the modules, their dependencies, aggregate roots and value objects; the hexagonal
 * roles are read from the jMolecules annotations on each class or its packages.
 *
 * <p>A business module holds at least one aggregate root, value object or port; any other module is
 * cross-cutting.
 */
final class DomainModelPage {

  private static final Pattern JAVADOC = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL);
  private static final Pattern FIRST_SENTENCE = Pattern.compile("^(.*?\\.)(\\s|$)");

  private final ApplicationModules modules;
  private final Path sources;

  /** Describes these modules, reading each module's description from these Java sources. */
  DomainModelPage(ApplicationModules modules, Path sources) {
    this.modules = modules;
    this.sources = sources;
  }

  /** Renders the page; fails naming the modules whose package has no Javadoc. */
  String render() {
    List<String> undescribed =
        modules.stream()
            .filter(module -> description(module).isEmpty())
            .map(ApplicationModule::getDisplayName)
            .toList();
    if (!undescribed.isEmpty()) {
      throw new IllegalStateException(
          "Every module needs a Javadoc on its package-info.java; missing for " + undescribed);
    }
    StringBuilder page = new StringBuilder();
    page.append("# Domain model\n\n")
        .append(
            "Generated from the code by `./gradlew :apps:services:api:docsDomainModel`: the modules come"
                + " from Spring Modulith, the roles from the jMolecules annotations. Do not edit"
                + " this page; change the code or its annotations.\n\n")
        .append("## Modules\n\n")
        .append("An arrow reads \"depends on\".\n\n")
        .append(moduleGraph())
        .append("\n## Business modules\n\n")
        .append(
            "Modules holding an aggregate root, a value object or a port: their model, and how a"
                + " request travels through their hexagon.\n");
    sortedModules().filter(this::isBusiness).forEach(module -> page.append(business(module)));
    page.append("\n## Cross-cutting modules\n\n")
        .append(
            "Technical modules without a domain model: what they provide, and who uses them.\n");
    sortedModules()
        .filter(module -> !isBusiness(module))
        .forEach(module -> page.append(crossCutting(module)));
    return page.toString();
  }

  /** Writes the rendered page to this file, creating its directory. */
  void writeTo(Path file) {
    try {
      Files.createDirectories(file.toAbsolutePath().getParent());
      Files.writeString(file, render());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String moduleGraph() {
    StringBuilder graph = new StringBuilder("```mermaid\nflowchart LR\n");
    sortedModules()
        .forEach(
            module -> graph.append("  %s[%s]\n".formatted(id(module), module.getDisplayName())));
    sortedModules()
        .forEach(
            module ->
                module
                    .getDirectDependencies(modules)
                    .uniqueModules()
                    .sorted(Comparator.comparing(ApplicationModule::getDisplayName))
                    .forEach(
                        target -> graph.append("  %s --> %s\n".formatted(id(module), id(target)))));
    return graph.append("```\n").toString();
  }

  private String business(ApplicationModule module) {
    return "\n### %s\n\n%s\n\n#### Model\n\n%s\n#### Request flow\n\n%s"
        .formatted(
            module.getDisplayName(),
            description(module).orElseThrow(),
            classDiagram(module),
            requestFlow(module));
  }

  private String classDiagram(ApplicationModule module) {
    List<JavaClass> aggregateRoots = domainTypes(module.getAggregateRoots());
    List<JavaClass> valueObjects = domainTypes(module.getValueTypes());
    Set<JavaClass> model = new LinkedHashSet<>(aggregateRoots);
    model.addAll(valueObjects);
    List<JavaClass> ports = withRole(module, SecondaryPort.class).toList();
    StringBuilder diagram = new StringBuilder("```mermaid\nclassDiagram\n");
    aggregateRoots.forEach(type -> diagram.append(classWithStereotype(type, "aggregate root")));
    valueObjects.forEach(type -> diagram.append(classWithStereotype(type, "value object")));
    ports.forEach(port -> diagram.append(classWithStereotype(port, "secondary port")));
    for (JavaClass owner : model) {
      owner.getFields().stream()
          .filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
          .filter(field -> model.contains(field.getRawType()))
          .sorted(Comparator.comparing(JavaField::getName))
          .forEach(
              field ->
                  diagram.append(
                      "  %s *-- %s : %s\n"
                          .formatted(
                              owner.getSimpleName(),
                              field.getRawType().getSimpleName(),
                              field.getName())));
    }
    for (JavaClass port : ports) {
      dependenciesOf(port)
          .filter(aggregateRoots::contains)
          .forEach(
              target ->
                  diagram.append(
                      "  %s ..> %s\n".formatted(port.getSimpleName(), target.getSimpleName())));
    }
    return diagram.append("```\n").toString();
  }

  private String requestFlow(ApplicationModule module) {
    List<JavaClass> ports = withRole(module, SecondaryPort.class).toList();
    List<JavaClass> useCases =
        withRole(module, PrimaryPort.class)
            .filter(useCase -> dependenciesOf(useCase).anyMatch(ports::contains))
            .toList();
    List<JavaClass> drivingAdapters =
        withRole(module, PrimaryAdapter.class)
            .filter(adapter -> dependenciesOf(adapter).anyMatch(useCases::contains))
            .toList();
    List<JavaClass> drivenAdapters =
        withRole(module, SecondaryAdapter.class)
            .filter(adapter -> adapter.getAllRawInterfaces().stream().anyMatch(ports::contains))
            .toList();
    StringBuilder flow = new StringBuilder("```mermaid\nflowchart LR\n");
    flow.append(subgraph("primaryAdapters", "Primary adapters", drivingAdapters))
        .append(subgraph("useCases", "Use cases (primary ports)", useCases))
        .append(subgraph("secondaryPorts", "Secondary ports", ports))
        .append(subgraph("secondaryAdapters", "Secondary adapters", drivenAdapters));
    drivingAdapters.forEach(adapter -> flow.append(edges(adapter, useCases)));
    useCases.forEach(useCase -> flow.append(edges(useCase, ports)));
    drivenAdapters.forEach(
        adapter ->
            adapter.getAllRawInterfaces().stream()
                .filter(ports::contains)
                .forEach(
                    port ->
                        flow.append(
                            "  %s -. implements .-> %s\n"
                                .formatted(adapter.getSimpleName(), port.getSimpleName()))));
    return flow.append("```\n").toString();
  }

  private String crossCutting(ApplicationModule module) {
    StringBuilder section =
        new StringBuilder(
            "\n### %s\n\n%s\n\n"
                .formatted(module.getDisplayName(), description(module).orElseThrow()));
    List<SpringBean> beans = module.getSpringBeans();
    Set<JavaClass> beanTypes =
        beans.stream().map(SpringBean::getType).collect(toCollection(LinkedHashSet::new));
    if (!beans.isEmpty()) {
      section.append("Spring beans, by stereotype:\n\n");
      new TreeMap<>(beans.stream().map(SpringBean::getType).collect(groupingBy(this::stereotype)))
          .forEach(
              (stereotype, types) ->
                  section.append("- `%s`: %s\n".formatted(stereotype, codeList(types))));
      section.append('\n');
    }
    List<JavaClass> publicTypes =
        classes(module)
            .filter(type -> type.getModifiers().contains(JavaModifier.PUBLIC))
            .filter(module::isExposed)
            .filter(type -> !beanTypes.contains(type))
            .toList();
    if (!publicTypes.isEmpty()) {
      section.append("Other public types: %s\n\n".formatted(codeList(publicTypes)));
    }
    List<String> users =
        modules.stream()
            .filter(other -> other.getDirectDependencies(modules).contains(module))
            .map(ApplicationModule::getDisplayName)
            .toList();
    section.append(
        users.isEmpty()
            ? "Used by no other module.\n"
            : "Used by: %s.\n".formatted(String.join(", ", users)));
    return section.toString();
  }

  private boolean isBusiness(ApplicationModule module) {
    return !domainTypes(module.getAggregateRoots()).isEmpty()
        || !domainTypes(module.getValueTypes()).isEmpty()
        || withRole(module, PrimaryPort.class).findAny().isPresent()
        || withRole(module, SecondaryPort.class).findAny().isPresent();
  }

  /** The first sentence of the Javadoc on the module's {@code package-info.java}. */
  private Optional<String> description(ApplicationModule module) {
    Path packageInfo =
        sources
            .resolve(module.getBasePackage().getName().replace('.', '/'))
            .resolve("package-info.java");
    if (!Files.exists(packageInfo)) {
      return Optional.empty();
    }
    String source;
    try {
      source = Files.readString(packageInfo);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Matcher javadoc = JAVADOC.matcher(source);
    if (!javadoc.find()) {
      return Optional.empty();
    }
    String text =
        javadoc
            .group(1)
            .lines()
            .map(line -> line.strip().replaceFirst("^\\*\\s?", ""))
            .takeWhile(line -> !line.startsWith("<p>") && !line.startsWith("@"))
            .collect(joining(" "))
            .strip();
    Matcher sentence = FIRST_SENTENCE.matcher(text);
    return text.isEmpty()
        ? Optional.empty()
        : Optional.of(sentence.find() ? sentence.group(1) : text);
  }

  private Stream<ApplicationModule> sortedModules() {
    return modules.stream().sorted(Comparator.comparing(ApplicationModule::getDisplayName));
  }

  /**
   * Drops the adapters' types: Modulith also counts a JPA entity behind a Spring Data repository as
   * an aggregate root, but it is a persistence detail, not the domain model.
   */
  private static List<JavaClass> domainTypes(List<JavaClass> types) {
    return types.stream()
        .filter(type -> !hasRole(type, PrimaryAdapter.class))
        .filter(type -> !hasRole(type, SecondaryAdapter.class))
        .sorted(Comparator.comparing(JavaClass::getSimpleName))
        .toList();
  }

  private Stream<JavaClass> classes(ApplicationModule module) {
    return module.getBasePackage().stream()
        .filter(JavaClass::isTopLevelClass)
        .filter(type -> !type.getSimpleName().equals("package-info"))
        .sorted(Comparator.comparing(JavaClass::getSimpleName));
  }

  /** The module's classes playing this hexagonal role, declared on them or on a package above. */
  private Stream<JavaClass> withRole(ApplicationModule module, Class<? extends Annotation> role) {
    return classes(module).filter(type -> hasRole(type, role));
  }

  private static boolean hasRole(JavaClass type, Class<? extends Annotation> role) {
    if (type.isAnnotatedWith(role)) {
      return true;
    }
    Optional<JavaPackage> current = Optional.of(type.getPackage());
    while (current.isPresent()) {
      if (current.get().isAnnotatedWith(role)) {
        return true;
      }
      current = current.get().getParent();
    }
    return false;
  }

  private static Stream<JavaClass> dependenciesOf(JavaClass type) {
    return type.getDirectDependenciesFromSelf().stream()
        .map(dependency -> dependency.getTargetClass().getBaseComponentType())
        .distinct();
  }

  /** The Spring stereotype annotation a bean is declared with, such as {@code @Configuration}. */
  private String stereotype(JavaClass type) {
    return type.getAnnotations().stream()
        .map(annotation -> annotation.getRawType())
        .filter(
            annotation ->
                annotation.isEquivalentTo(Component.class)
                    || annotation.isMetaAnnotatedWith(Component.class))
        .map(annotation -> "@" + annotation.getSimpleName())
        .findFirst()
        .orElse("@Bean");
  }

  private static String classWithStereotype(JavaClass type, String stereotype) {
    return "  class %s {\n    <<%s>>\n  }\n".formatted(type.getSimpleName(), stereotype);
  }

  private static String subgraph(String id, String title, List<JavaClass> members) {
    if (members.isEmpty()) {
      return "";
    }
    StringBuilder subgraph = new StringBuilder("  subgraph %s [\"%s\"]\n".formatted(id, title));
    members.forEach(member -> subgraph.append("    %s\n".formatted(member.getSimpleName())));
    return subgraph.append("  end\n").toString();
  }

  /** An arrow from the source to each of these targets it depends on. */
  private static String edges(JavaClass source, List<JavaClass> targets) {
    return dependenciesOf(source)
        .filter(targets::contains)
        .sorted(Comparator.comparing(JavaClass::getSimpleName))
        .map(target -> "  %s --> %s\n".formatted(source.getSimpleName(), target.getSimpleName()))
        .collect(joining());
  }

  private static String codeList(List<JavaClass> types) {
    return types.stream()
        .map(type -> "`" + type.getSimpleName() + "`")
        .sorted()
        .collect(joining(", "));
  }

  private static String id(ApplicationModule module) {
    return module.getIdentifier().toString();
  }
}
