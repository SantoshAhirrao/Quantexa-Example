package com.quantexa.example.gateway

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.test.context.{ActiveProfiles, ContextConfiguration, TestContextManager}

@SpringBootTest
@ContextConfiguration(
  classes = Array(classOf[MockSamlBeans]),
  loader = classOf[AnnotationConfigContextLoader])
@RunWith(classOf[JUnitRunner])
@ActiveProfiles(Array("saml"))
class GatewayWiringTest extends FlatSpec {

  var appContext: ApplicationContext = null

  @Autowired
  def applicationContext(applicationContext: ApplicationContext) = {
    this.appContext = applicationContext
  }

  new TestContextManager(this.getClass()).prepareTestInstance(this)

  "context" should "have 2 beans for saml profile" in {
    val beanNames: List[String] = appContext.getBeanDefinitionNames.toList

    assert(beanNames.filter(_.endsWith("Bean")).length == 2)
  }

  "context" should "have just saml configuration profile applied" in {
    val envs: Array[String] = appContext.getEnvironment.getActiveProfiles

    assert(envs.filter(_.contains("saml")).length == 1)
  }
}

case class AuthScheme(mechanism: Mechanism)
sealed trait Mechanism
case class LDAPScheme() extends Mechanism
case class SAMLScheme() extends Mechanism
case class EmbedLDAPScheme() extends Mechanism
