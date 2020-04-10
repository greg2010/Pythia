package org.kys.pythia.uwapi.endpoints

import com.typesafe.scalalogging.LazyLogging
import sttp.client._
import sttp.model.Uri


abstract class BaseApi(apiKey: String) extends LazyLogging {

  def constructUrl(path: List[String], params: Map[String, String]): Uri =
    uri"https://api.uwaterloo.ca/?key=$apiKey&$params".path("v2" +: path)
  def constructUrl(path: List[String]): Uri = constructUrl(path, Map())

  def onError[T <: Throwable](method: String, err: ResponseError[T]): ResponseError[T] = {
    err match {
      case HttpError(body) =>
        logger.error(s"Got an http error on method $method body $body")
      case DeserializationError(body, ex) =>
        logger.error(s"Failed to deserialize response on method $method errorBody $body", ex)
    }
    err
  }
}
