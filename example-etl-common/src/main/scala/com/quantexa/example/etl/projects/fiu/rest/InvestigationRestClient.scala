package com.quantexa.example.etl.projects.fiu.rest

import com.quantexa.engSpark.model.ResolverModel.Request
import com.quantexa.graph.script.utils.RestScalaClient._
import com.quantexa.investigation.api.GraphCustodianProtocol.InitialiseGraphResponse
import com.quantexa.example.etl.projects.fiu.rest.RestClientUtils._
import io.circe.Json
import io.circe.generic.auto._
import cats.effect.IO
import com.quantexa.graph.script.utils.{RestScalaClient, SslContexts}
import org.apache.spark.sql

object InvestigationRestClient {

  case class InvestigationRequest(
                                   resolverRequest: Request
                                   , lazyInit: Option[Boolean]
                                   , name: Option[String]
                                   , natures: Option[Seq[InvestigationNature]]
                                 )

  case class InvestigationNature(
                                  name:String
                                )

  implicit val investigationRequestEncoder: sql.Encoder[InvestigationRequest] = org.apache.spark.sql.Encoders.kryo[InvestigationRequest]

  /**
    * Loads a dataset worth of investigations to the UI
    *
    * @param data           A json dataset describing each of the investigations to be loaded
    * @param loginUrl       The url of the login for obtaining an access token
    * @param explorerUrl    The url of the explorer for loading an investigation too it
    * @return
    *
    */
  def loadInvestigation(data: Json, loginUrl: String, explorerUrl: String, username: String, password: String): IO[InitialiseGraphResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + "/investigation?lazyInit=true")
      token <- getLoginToken(loginUrl, username, password)
      result <- post[InitialiseGraphResponse](data, explorerUri, RestScalaClient.defaultHttpClient, token)
    } yield result
  }
}