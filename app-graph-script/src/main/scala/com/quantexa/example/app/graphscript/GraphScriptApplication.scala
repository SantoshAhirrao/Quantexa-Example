package com.quantexa.example.app.graphscript

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.base.Predicate
import com.quantexa.resolver.core.Properties
import com.quantexa.resource.annotations.NoSwaggerDoc
import com.quantexa.security.utils.jwt.EnableJwtHttpSession
import com.quantexa.spring.boot.QuantexaSpringApplication
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.spring.autoconfigure.MeterRegistryCustomizer
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.boot.autoconfigure.{EnableAutoConfiguration, SpringBootApplication}
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration}
import org.springframework.core.annotation.Order
import org.springframework.retry.annotation.EnableRetry
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.RequestHandler
import springfox.documentation.builders.PathSelectors
import springfox.documentation.service.{ApiInfo, ApiKey}
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

import scala.collection.JavaConverters.seqAsJavaListConverter

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(
  basePackages = Array(
    "com.quantexa.example.graph.scripting.rest"))
@EnableJwtHttpSession
@EnableRetry
@EnableSwagger2
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
class GraphScriptApplication extends WebSecurityConfigurerAdapter {

  @Autowired
  def configureJackson(mapper: ObjectMapper): Unit = {
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JodaModule)
    mapper.registerModule(Properties.TypePreservingPropertiesJacksonModule)
    mapper.configure(
      com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
      false)
  }
  @Bean
  def metricsCommonTags(@Value("${spring.application.name}") appName: String): MeterRegistryCustomizer[MeterRegistry] = {
    // TODO: move this to config (management.metrics.tags.application: appName) once 1.1.0 of micrometer is released
    new MeterRegistryCustomizer[MeterRegistry] {
      override def customize(registry: MeterRegistry): Unit = registry.config().commonTags("application", appName)
    }
  }

  override def configure(http: HttpSecurity) = {
    http
      .httpBasic()
      .and()
      .authorizeRequests()
      .antMatchers("/v2/**","/swagger-resources/**", "/swagger-ui.html")
      .permitAll()
      .antMatchers("/**")
      .hasRole("USER")
      .anyRequest()
      .authenticated()
      .and()
      .csrf()
      .disable()
  }

  @Bean
  def api = new Docket(DocumentationType.SWAGGER_2)
    .select
    .apis(new Predicate[RequestHandler]() {
      override def apply(input: RequestHandler): Boolean =
        input.declaringClass.isAnnotationPresent(classOf[RestController]) && !input.isAnnotatedWith(classOf[NoSwaggerDoc])
    })
    .paths(PathSelectors.any)
    .build
    .securitySchemes(List(new ApiKey("Authorization", "Authorization", "header")).asJava)
    .apiInfo(new ApiInfo(
      "app-graph-script REST API",
      "app-graph-script endpoints",
      "1.0",
      "Terms of service",
      "Quantexa Research & Development",
      "License of API",
      "http://www.quantexa.com"))
}

object GraphScriptApplication {
  def main(args: Array[String]): Unit = {
    QuantexaSpringApplication(classOf[GraphScriptApplication], args)
  }
}
