package dev.ylp.buildlogic

/** Java sources shared by the fixtures, already in google-java-format. */
object JavaSources {
    const val GREETER = """package dev.ylp.sample;

public final class Greeter {
  private Greeter() {}

  public static String greet(String name) {
    return "Hello " + name;
  }
}
"""

    const val GREETER_TEST = """package dev.ylp.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GreeterTest {
  @Test
  void greetsByName() {
    assertEquals("Hello Ada", Greeter.greet("Ada"));
  }
}
"""

    fun javaProject(plugin: String) =
        """
        plugins {
            id("$plugin")
        }
        """.trimIndent() + "\n"
}
