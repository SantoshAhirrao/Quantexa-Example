package com.quantexa.example.etl.projects.fiu.rest

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration
import com.quantexa.graph.script.utils.RestScalaClient._
import io.circe.{Decoder, Json, KeyEncoder}
import cats.effect.{IO, Resource}
import com.quantexa.graph.script.utils.{AuthToken, RestScalaClient, SslContexts}
import com.quantexa.graph.script.utils.RestScalaClient.{defaultRetryDuration, retryWithBackoff}
import org.http4s.{Header, Headers, Method, Request, Uri}
import org.http4s.circe.jsonOf
import org.http4s.circe._
import com.quantexa.security.api.SecurityModel.{RoleId, UserId}
import org.http4s.client.Client


object RestClientUtils {
  /**
    * Turns the string representation of an address to a URI
    *
    * @param address    The address being transformed
    * @return
    * IO Computation storing the URI
    */
  def stringToUri(address: String): IO[Uri] = IO.fromEither(Uri.fromString(address))

  /**
    * Generates the UI access token
    *
    * @return
    * IO Computation storing the UI access token
    */
  def getLoginToken(loginUrl: String, username: String, password: String, loginTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS)): IO[AuthToken] = {
    for {
      loginUri <- stringToUri(loginUrl)
      _ = logger.info(s"Attempting to log in")
      token <- timeout(login(loginUri, username, password, RestScalaClient.defaultHttpClient), loginTimeout, s"Login")
      _ = logger.info(s"Successfully logged in, token: $token")
    } yield token
  }

  //TODO: These should be added to the graph scripting rest client
  /**
    * Sends a put request to the specified URI
    *
    * @param data       Json package to send in put request
    * @param uri
    * @param authToken  Authentication token for server
    * @param retries    How many times to try the put request
    * @param decoderT   Implicit decoder for returned type
    * @tparam T         Expected type of server response
    * @return
    * IO of the response from the server
    */
  def put[T](data: Json,
             uri: Uri,
             httpClient: Resource[IO, Client[IO]],
             authToken: AuthToken,
             retries: Int = 3)(implicit decoderT: Decoder[T]): IO[T] = {

    val putRequest: IO[Request[IO]] = generatePutRequest(data, authToken, uri)

    val putIO = httpClient.use(httpClient => httpClient.expect[T](putRequest)(jsonOf[IO, T]))

    retryWithBackoff(putIO, defaultRetryDuration, retries, "Put input")
  }

  /**
    * Generates a put request from Json
    *
    * @param data       Json package to send in put request
    * @param authToken  Authentication token for server
    * @param parsedUrl  Uri for server
    * @return
    * IO request for the server
    */
  def generatePutRequest(data: Json, authToken: AuthToken, parsedUrl: Uri): IO[Request[IO]] = {
    IO(Request[IO](
      method = Method.PUT,
      uri = parsedUrl,
      headers = Headers(Header("Content-Type", "application/json"),
        Header("Authorization", s"Bearer ${authToken.token}")))
      .withEntity(data))
  }

  //Implicits for getting a SubjectPrivilegeSetMapping to be turned into Json as required by circe
  implicit val roleKeyEncoder: KeyEncoder[RoleId] = new KeyEncoder[RoleId] {
    override def apply(role: RoleId): String = role.toString
  }

  implicit val userKeyEncoder: KeyEncoder[UserId] = new KeyEncoder[UserId] {
    override def apply(user: UserId): String = user.toString
  }
}
