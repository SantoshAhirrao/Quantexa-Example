package com.quantexa.example.gateway

import org.springframework.context.annotation.{Bean, Configuration, Primary}
import com.quantexa.gateway.auth.saml._
import com.quantexa.gateway.auth.ldap._

@Configuration
class MockSamlBeans {

  @Bean
  def plainBean: Mechanism = new EmbedLDAPScheme

  @SAML
  @Primary
  @Bean
  def conditionalSAMLBean: Mechanism = new SAMLScheme

  @LDAP
  @Primary
  @Bean
  def conditionalLDAPBean: Mechanism = new LDAPScheme

  @Bean
  def authScheme(mechanism: Mechanism): AuthScheme = new AuthScheme(mechanism)
}
