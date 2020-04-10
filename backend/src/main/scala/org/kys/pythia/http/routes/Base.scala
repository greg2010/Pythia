package org.kys.pythia.http.routes

import cats.effect.IO
import org.http4s.HttpRoutes
import org.kys.pythia.ScheduleNotifierController


object Base {

  def apply(scheduleNotifierController: ScheduleNotifierController): HttpRoutes[IO] = {
    val premades = Sections(scheduleNotifierController)

    premades.toRoutes(identity)
  }
}
