package dev.ylp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the API: a Spring Modulith whose modules are the packages below this one. */
@SpringBootApplication
public class ApiApplication {

  /** Starts the application. */
  public static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args);
  }
}
