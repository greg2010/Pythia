package org.kys.pythia.http.routes

import cats.effect.IO
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import org.kys.pythia.ScheduleNotifierController


object Sections {

  def apply(scheduleNotifierController: ScheduleNotifierController): RhoRoutes[IO] = {
    new RhoRoutes[IO] {

      "Endpoint to add a section by its id and term id to monitoring" **
        POST / "terms" / pathVar[Int]("termId") /
        "sections" / pathVar[Int]("sectionId") +?
        param[String]("email") |>> { (termId: Int, sectionId: Int, email: String) =>
        scheduleNotifierController.createCourseRecord(sectionId, termId, email).flatMap(Created(_))
      }

      "Endpoint to remove a section by its id and term id from monitoring" **
        DELETE / "terms" / pathVar[Int]("termId") /
        "sections" / pathVar[Int]("sectionId") +?
        param[String]("email") |>> { (termId: Int, sectionId: Int, email: String) =>
        scheduleNotifierController.deleteCourseRecord(sectionId, termId, email).map(_ => NoContent.apply)
      }
    }
  }
}
