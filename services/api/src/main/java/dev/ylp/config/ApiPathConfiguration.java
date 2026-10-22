package dev.ylp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves every REST controller under the contract's base path. The generated interfaces only carry
 * the paths relative to the contract's {@code servers} entry.
 */
@Configuration
public class ApiPathConfiguration implements WebMvcConfigurer {

  /** Base path of the API, as declared in the contract's {@code servers}. */
  public static final String BASE_PATH = "/api/v1";

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(BASE_PATH, HandlerTypePredicate.forAnnotation(RestController.class));
  }
}
