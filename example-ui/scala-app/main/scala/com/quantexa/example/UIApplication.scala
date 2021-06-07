package com.quantexa.example

import java.util.EnumSet

import com.quantexa.security.utils.jwt.EnableJwtHttpSession
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.spring.autoconfigure.MeterRegistryCustomizer
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import com.quantexa.spring.boot.QuantexaSpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.boot.autoconfigure.web.ErrorController
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry

@SpringBootApplication
@Controller
@EnableDiscoveryClient
class UIApplication extends ErrorController {

  override def getErrorPath: String = "/error"

  @RequestMapping(value = Array("/"))
  def rootForward(request: HttpServletRequest, response: HttpServletResponse) = {
    request.getRequestDispatcher("/index.html").forward(request, response)
  }

  @RequestMapping(value = Array("/login"))
  def loginForward(request: HttpServletRequest, response: HttpServletResponse) = {
    request.getRequestDispatcher("/login/index.html").forward(request, response)
  }

  @RequestMapping(Array("/error"))
  def errorForward(request: HttpServletRequest, response: HttpServletResponse) = {
    request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) match {
      case url: String if url.matches("\\/login\\/.*") => loginForward(request, response)
      case _ => rootForward(request, response)
    }
  }

  def addResourceHandlers(registry: ResourceHandlerRegistry): Unit = {
    registry
      .addResourceHandler("/")
      .addResourceLocations(
        "classpath:/BOOT-INF/classes/static/**",
        "classpath:/BOOT-INF/classes/static",
        "classpath:/BOOT-INF/classes/",
        "classpath:/BOOT-INF/classes/static/login",
        "classpath:/BOOT-INF/classes/static/login/**"
      )
  }
}

@EnableJwtHttpSession
@Configuration
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
class SecurityConfiguration extends WebSecurityConfigurerAdapter {
  override def configure(http: HttpSecurity) = {
    http
      .httpBasic()
      .and()
      .authorizeRequests()
      .antMatchers("/**")
      .permitAll()
      .and()
      .authorizeRequests()
      //.antMatchers("/app/index.html", "/", "/login/index.html", "/index.html", "/app", "/login")
      .antMatchers("/**")
      .permitAll()
      .anyRequest()
      .hasRole("USER")
      .and()
      .authorizeRequests()
      .antMatchers(HttpMethod.POST, "/**")
      .hasRole("USER")
      .anyRequest()
      .authenticated()
  }

  @Bean
  def metricsCommonTags(@Value("${spring.application.name}") appName: String): MeterRegistryCustomizer[MeterRegistry] = {
    // TODO: move this to config (management.metrics.tags.application: appName) once 1.1.0 of micrometer is released
    new MeterRegistryCustomizer[MeterRegistry] {
      override def customize(registry: MeterRegistry): Unit = registry.config().commonTags("application", appName)
    }
  }
}

@Configuration
class WebConfigurer extends ServletContextInitializer {
  val log = LoggerFactory.getLogger(this.getClass)

  override def onStartup(sc: ServletContext): Unit = {
    log.info(s"Configuring web application")
    val disps = EnumSet.of(
      DispatcherType.REQUEST,
      DispatcherType.FORWARD,
      DispatcherType.ASYNC
    )

    val staticResourcesProductionFilter = sc.addFilter(
      "staticResourcesProductionFilter",
      new StaticResourcesProductionFilter()
    )
    staticResourcesProductionFilter.addMappingForServletNames(disps, true, "/**")
    staticResourcesProductionFilter.addMappingForServletNames(disps, true, "/index.html")
    staticResourcesProductionFilter.addMappingForServletNames(disps, true, "/assets")
    staticResourcesProductionFilter.addMappingForServletNames(disps, true, "/assets/**")
    staticResourcesProductionFilter.addMappingForServletNames(disps, true, "/login")
    staticResourcesProductionFilter.addMappingForServletNames(disps, true, "/login/**")
    staticResourcesProductionFilter.addMappingForServletNames(disps, true, "/login/index.html")
    staticResourcesProductionFilter.setAsyncSupported(true)
  }
}

class StaticResourcesProductionFilter extends Filter {
  override def init(config: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpRequest = request.asInstanceOf[HttpServletRequest]
    val contextPath = httpRequest.getContextPath
    var requestUri = httpRequest.getRequestURI
    if (requestUri == "/") {
      requestUri = "/index.html"
    }
    if (requestUri == "/login") {
      requestUri = "/login/index.html"
    }
    val newUri = "/app" + requestUri
    request.getRequestDispatcher(newUri).forward(request, response)
  }
}

object UIApplication {
  def main(args: Array[String]): Unit = {
    QuantexaSpringApplication(classOf[UIApplication], args)
  }
}
