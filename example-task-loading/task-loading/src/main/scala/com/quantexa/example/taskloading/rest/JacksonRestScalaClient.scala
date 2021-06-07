package com.quantexa.example.taskloading.rest

import cats.effect._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.quantexa.graph.script.utils.{AuthToken, RestScalaClient}
import io.circe.Decoder
import io.circe.parser._
import org.http4s.circe._
import org.http4s.{Header, Headers, Method, Request, Uri}
import org.http4s.client.Client

/**
  * Extension to RestScalaClient provided by Quantexa Accelerators
  * Jackson is used to encode the requests as certain types aren't correctly encodable using Circe 'out of the box'
  */
object JacksonRestScalaClient {

  import RestScalaClient._

  val objectMapper = new ObjectMapper
  objectMapper.registerModule(DefaultScalaModule)

  def method[T](method: org.http4s.Method)(
    data: Product,
    uri: Uri,
    httpClient: Resource[IO, Client[IO]],
    authToken: AuthToken,
    retries: Int = 3)(implicit decoderT: Decoder[T]): IO[T] = {

    val request: IO[Request[IO]] = generateJacksonRequest(method)(data, authToken, uri)

    val requestIO: IO[T] = httpClient.use(client =>
      client.expect[T](request)(jsonOf[IO, T]))

    retryWithBackoff(requestIO, defaultRetryDuration, retries,
      s"Method: ${method.name}, URI: ${uri.renderString},\n Data: ${data.toString}")
  }

  def get[T](data: Product,
             uri: Uri,
             httpClient: Resource[IO, Client[IO]],
             authToken: AuthToken,
             retries: Int = 3)(implicit decoderT: Decoder[T]): IO[T] =
    method[T](Method.GET)(data, uri, httpClient, authToken, retries)

  def put[T](data: Product,
             uri: Uri,
             httpClient: Resource[IO, Client[IO]],
             authToken: AuthToken,
             retries: Int = 3)(implicit decoderT: Decoder[T]): IO[T] =
    method[T](Method.PUT)(data, uri, httpClient, authToken, retries)

  def post[T](data: Product,
              uri: Uri,
              httpClient: Resource[IO, Client[IO]],
              authToken: AuthToken,
              retries: Int = 3)(implicit decoderT: Decoder[T]): IO[T] =
    method[T](Method.POST)(data, uri, httpClient, authToken, retries)

  private def generateJacksonRequest(method: org.http4s.Method)(product: Product, token: AuthToken, uri: Uri): IO[Request[IO]] = {
    val jsonAsString = objectMapper.writeValueAsString(product)

    println(jsonAsString)

    val json = parse(jsonAsString) match {
      case Left(err) => throw err.underlying
      case Right(parsedJson) => parsedJson
    }

    IO(Request[IO](
      method = method,
      uri = uri,
      headers = Headers(Header("Content-Type", "application/json"),
        Header("Authorization", s"Bearer ${token.token}")))
      .withEntity(json))
  }

}
