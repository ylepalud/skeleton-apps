package dev.ylp;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("dev.ylp");

  @Test
  @DisplayName("Ports and adapters respect the hexagonal dependency rules")
  void portsAndAdaptersRespectTheHexagonalDependencyRules() {
    // When / Then
    JMoleculesArchitectureRules.ensureHexagonal().check(CLASSES);
  }

  @Test
  @DisplayName("Aggregates and entities respect the DDD building block rules")
  void aggregatesAndEntitiesRespectTheDddBuildingBlockRules() {
    // When / Then
    JMoleculesDddRules.all().check(CLASSES);
  }

  @Test
  @DisplayName("The domain and application layers depend neither on Spring nor on adapters")
  void domainAndApplicationDependNeitherOnSpringNorOnAdapters() {
    // When / Then: @Transactional on use cases is the only Spring type allowed
    noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..application..")
        .should()
        .dependOnClassesThat(
            resideInAnyPackage("org.springframework..", "..adapter..")
                .and(not(resideInAPackage("org.springframework.transaction.annotation"))))
        .check(CLASSES);
  }
}
