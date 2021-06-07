package com.quantexa.example.gateway

import java.security.Principal
import java.util.{Map => JavaMap}

import com.quantexa.gateway._
import com.quantexa.gateway.auth.AuthConfiguration
import com.quantexa.gateway.auth.embed.EmbeddedLDAPModule
import com.quantexa.gateway.auth.ldap._
import com.quantexa.gateway.auth.saml._

import scala.collection.JavaConversions.mapAsJavaMap
import org.audit4j.core.annotation.Audit
import org.audit4j.core.annotation.DeIdentify
import org.jboss.aerogear.security.otp.api.Base32
import org.springframework.beans.factory.annotation.{Autowired, Value}
import com.quantexa.spring.boot.QuantexaSpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.core.annotation.Order
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.{EnableWebSecurity, WebSecurityConfigurerAdapter}
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation._
import com.quantexa.gateway.auth.{LdapDao, TotpWebAuthenticationDetailsSource}
import javax.servlet.http.{Cookie, HttpServletRequest}
import com.quantexa.resource.utils.auth.AuthSessionSettings
import com.quantexa.security.utils.jwt.{Authenticator, EnableJwtHttpSession, TokenProvider}
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.spring.autoconfigure.MeterRegistryCustomizer
import javax.servlet.RequestDispatcher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.boot.autoconfigure.web.ErrorController
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext
import org.springframework.retry.annotation.EnableRetry
import org.springframework.stereotype.Controller
import org.springframework.util.StringUtils
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration, Primary}
import org.springframework.security.web.authentication.{AuthenticationFailureHandler, SimpleUrlAuthenticationFailureHandler}

import scala.collection.JavaConversions.mapAsJavaMap
import scala.util.Try


@SpringBootApplication
@Controller
@EnableDiscoveryClient
@EnableZuulProxy
@EnableRetry
@ComponentScan(
  basePackages = Array(
    "com.quantexa.gateway",
    "com.quantexa.example.gateway",
    "com.quantexa.security.utils.jwt"
  )
)
class GatewayApplication(authSessionSettings: AuthSessionSettings, tokenProvider: TokenProvider, ldapDao: LdapDao) extends ErrorController {

  private val logger = LoggerFactory.getLogger(getClass)
  override def getErrorPath: String = "/error"

  var appContext: EmbeddedWebApplicationContext = null

  @Audit
  @RequestMapping(Array("/user"))
  @ResponseBody
  def user(user: Principal): JavaMap[String, Object] = {

    Map(
      "name" -> user.getName,
      "authorities" -> AuthorityUtils.authorityListToSet(
        user.asInstanceOf[Authentication].getAuthorities
      )
    )
  }

  @RequestMapping(value = Array("/api-root/getPropertyMappings"))
  @ResponseBody
  def getPropertyMappings: JavaMap[String, Any] = Map(
    "totpEnabled"       -> authSessionSettings.totpEnabled,
    "sessionTimeout"    -> authSessionSettings.sessionTimeout,
    "tokenIsRememberMe" -> authSessionSettings.tokenIsRememberMe
  )

  @Audit
  @RequestMapping(value = Array("/saveTotp/{secret}"), method = Array(RequestMethod.POST))
  @ResponseBody
  def save(@DeIdentify @PathVariable("secret") secret: String, principal: Principal) = {
    val userToUpdate = ldapDao.findByName(principal.getName).getOrElse(throw new UsernameNotFoundException(s"User '${principal.getName}' could not be found in LDAP to update TOTP"))
    ldapDao.update(userToUpdate.copy(totpSecret = Some(secret), totpTempCode = None))

    Map ("outcome" -> "success")
  }

  @RequestMapping(Array("/login"))
  def loginRouting(request: HttpServletRequest) = {
    if (isTokenValid(request)) {
      authSessionSettings.proxyPathPrefix orElse Option(request.getHeader("X-Forwarded-Prefix")).filter(_.trim.nonEmpty) match {
        case None            => "redirect:/"
        case Some(proxyPath) => s"redirect:/$proxyPath/"
      }
    } else {
      authSessionSettings.proxyPathPrefix orElse Option(request.getHeader("X-Forwarded-Prefix")).filter(_.trim.nonEmpty) match {
        case None            => "forward:/ui/login/index.html"
        case Some(proxyPath) => s"forward:/$proxyPath/ui/login/index.html"
      }
    }
  }

  @RequestMapping(Array("/"))
  def rootForward(request: HttpServletRequest) = {
    forwardUIRequest(request)
  }

  @RequestMapping(Array("/error"))
  def errorForward(request: HttpServletRequest) = {
    request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) match {
      case url: String if url.matches("\\/api\\/.*") =>
      case _                                         => forwardUIRequest(request)
    }
  }

  @Autowired
  def applicationContext(applicationContext: EmbeddedWebApplicationContext) = {
    this.appContext = applicationContext
  }

  private def forwardUIRequest(request: HttpServletRequest): String = {

    import scala.util.{Failure, Success}
    def isSamlEnabled(): Boolean = {
      Try(appContext.getBean("samlAuthModule").asInstanceOf[SAMLModule]) match {
        case Success(_) => true
        case Failure(exception) => false
      }
    }

    if (isTokenValid(request)) {
      authSessionSettings.proxyPathPrefix orElse Option(request.getHeader("X-Forwarded-Prefix")).filter(_.trim.nonEmpty) match {
        case None            => "forward:/ui/index.html"
        case Some(proxyPath) => s"forward:/$proxyPath/ui/index.html"
      }
    } else if (isSamlEnabled) {
      authSessionSettings.proxyPathPrefix orElse Option(request.getHeader("X-Forwarded-Prefix")).filter(_.trim.nonEmpty) match {
        case None            => "forward:/saml/login"
        case Some(proxyPath) => s"forward:$proxyPath/saml/login"
      }
    } else {
      authSessionSettings.proxyPathPrefix orElse Option(request.getHeader("X-Forwarded-Prefix")).filter(_.trim.nonEmpty) match {
        case None => "forward:ui/login/index.html"
        case Some(proxyPath) => s"forward:$proxyPath/ui/login/index.html"
      }
    }
  }

  private def getAuthTokenCookie(request: HttpServletRequest): Option[Cookie] = {
    val getCookies = request.getCookies()
    getCookies match {
      case cookies if cookies != null => cookies.find(_.getName == "authenticationToken")
      case _ => None
    }
  }

  private def resolveToken(request: HttpServletRequest): String = {
    val bearerToken = request.getHeader("Authorization")
    val getCookie = getAuthTokenCookie(request)
    (getCookie, bearerToken) match {
      case (Some(cookie), _) if !cookie.getValue.isEmpty => cookie.getValue
      case (_, token) if StringUtils.hasText(token) && token.startsWith("Bearer ") => token.drop(7)
      case _ => null
    }

  }

  private def isTokenValid(request: HttpServletRequest): Boolean = {
    tokenProvider.validateToken(resolveToken(request))
  }

  @Bean
  def metricsCommonTags(@Value("${spring.application.name}") appName: String): MeterRegistryCustomizer[MeterRegistry] = {
    // TODO: move this to config (management.metrics.tags.application: appName) once 1.1.0 of micrometer is released
    new MeterRegistryCustomizer[MeterRegistry] {
      override def customize(registry: MeterRegistry): Unit = registry.config().commonTags("application", appName)
    }
  }

  @SAML
  @Bean
  def samlAuthenticationFailureHandler: AuthenticationFailureHandler =  new SimpleUrlAuthenticationFailureHandler("/error")

  @Bean
  @Primary
  @SAML
  def samlAuthModule(authenticator: Authenticator,
                     authSessionSettings: AuthSessionSettings,
                     authConfig: AuthConfiguration,
                     sAMLAuthHolder: SAMLAuthHolder,
                     sAMlAuthenticationSuccessHandler: SAMLAuthenticationSuccessHandler,
                     samlAuthenticationFailureHandler: AuthenticationFailureHandler,
                     roleMappingSAMLUserDetailsService: SAMLRoleMappingUserDetailsService): AuthModule = {

    new SAMLModule(authenticator, authSessionSettings, authConfig, sAMLAuthHolder, sAMlAuthenticationSuccessHandler, samlAuthenticationFailureHandler, roleMappingSAMLUserDetailsService)
  }

  @Bean
  def embeddedLdapAuthModule(authenticator: Authenticator,
                             ldapContextSource: LdapContextSource,
                             authSessionSettings: AuthSessionSettings,
                             authenticationDetailsSource: TotpWebAuthenticationDetailsSource,
                             authConfig: AuthConfiguration): AuthModule = {

    new EmbeddedLDAPModule(authenticator, ldapContextSource, authSessionSettings, authenticationDetailsSource, authConfig)
  }

  @Bean
  def ldapTotpSettable: LDAPTotpConfigurable = new LDAPTotpConfigurer

  @Bean
  @Primary
  @LDAP
  def ldapAuthModule(authenticator: Authenticator,
                     ldapContextSource: LdapContextSource,
                     authSessionSettings: AuthSessionSettings,
                     authenticationDetailsSource: TotpWebAuthenticationDetailsSource,
                     authConfig: AuthConfiguration,
                     ldapRoleMappingService: LDAPRoleMappingUserService): AuthModule = {

    new LDAPModule(authenticator, ldapContextSource, authSessionSettings, authenticationDetailsSource, authConfig, ldapRoleMappingService)
  }
}

@Configuration
class FiltersConfig {

  @Bean
  def changeURIFilter = new com.quantexa.gateway.zuul.ChangeUriFilter
}

@EnableJwtHttpSession
@Configuration
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
@EnableWebSecurity
class SecurityConfiguration(authModule: AuthModule) extends WebSecurityConfigurerAdapter {


  override def configure(auth: AuthenticationManagerBuilder): Unit = {
    authModule.configAuth(auth)
  }

  override def configure(http: HttpSecurity): Unit = {
    authModule.configHttp(http)
  }
}

object GatewayApplication {
  def main(args: Array[String]): Unit = {
    QuantexaSpringApplication(classOf[GatewayApplication], args)
  }
}
