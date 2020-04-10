package org.kys.pythia.uwapi.endpoints

import io.circe
import io.circe.generic.auto._
import org.kys.pythia.uwapi.dto.UWResponse
import org.kys.pythia.uwapi.dto.terms.TermList
import sttp.client._
import sttp.client.circe._


class Terms(apiKey: String) extends BaseApi(apiKey) {

  def getTermsList: RequestT[Identity, Either[ResponseError[circe.Error], TermList], Nothing] = {
    val url = constructUrl(List("terms", "list.json"))
    basicRequest.get(url).response(asJson[UWResponse[TermList]]).mapResponse {
      case Right(r) => Right(r.data)
      case Left(ex) => Left(onError("getTermsList", ex))
    }
  }
}
