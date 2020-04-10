package org.kys.pythia.uwapi
import org.kys.pythia.uwapi.endpoints.{Courses, Terms}


class UWApi(apiKey: String) {
  val courses = new Courses(apiKey)
  val terms = new Terms(apiKey)
}
