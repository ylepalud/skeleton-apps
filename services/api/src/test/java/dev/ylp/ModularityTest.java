package dev.ylp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

  private final ApplicationModules modules = ApplicationModules.of(ApiApplication.class);

  @Test
  @DisplayName("Modules only depend on each other through their exposed API")
  void modulesOnlyDependOnEachOtherThroughTheirExposedApi() {
    // When / Then
    modules.verify();
  }

  @Test
  @DisplayName("The module documentation is generated")
  void moduleDocumentationIsGenerated() {
    // When / Then: C4 component diagrams and module canvases under build/spring-modulith-docs
    new Documenter(modules).writeDocumentation();
  }
}
