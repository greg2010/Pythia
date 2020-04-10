package org.kys.pythia.uwapi.endpoints

import sttp.client._
import sttp.client.circe._
import io.circe.generic.auto._
import org.kys.pythia.uwapi.dto.UWResponse
import org.kys.pythia.uwapi.dto.course.Schedule


class Courses(apiKey: String) extends BaseApi(apiKey) {

  def getCourseSchedule(classNumber: Int, term: Int)
  : RequestT[Identity, Either[ResponseError[Throwable], Schedule], Nothing] = {
    val url = constructUrl(List("courses", classNumber.toString, "schedule.json"), Map("term" -> term.toString))
    basicRequest.get(url).response(asJson[UWResponse[List[Schedule]]]).mapResponse {
      case Left(ex) => Left(onError("getCourseSchedule", ex))
      case Right(v) =>
        v.data.headOption match {
          case Some(r) => Right(r)
          case None => Left(DeserializationError("Bad data", new RuntimeException("Bad data")))
        }
    }
  }
}
