package org.kys.pythia


import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import doobie.util.ExecutionContexts
import org.kys.pythia.db.Database
import org.kys.pythia.uwapi.UWApi
import org.matthicks.mailgun.EmailAddress
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.kys.pythia.http.middleware.{ApacheLogging, ErrorHandler}
import org.kys.pythia.http.routes.Base

import scala.concurrent.duration._
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend



object Server extends IOApp with LazyLogging {
  case class ServerJobs(initDb: IO[Unit], scheduleNotifierController: ScheduleNotifierController, httpServer: IO[ExitCode])
  lazy val acquireConfigs: Resource[IO, ServerJobs] = for {
    config <- config.load
    fixedThreadPool <- ExecutionContexts.fixedThreadPool[IO](config.database.threadPoolSize)
    blocker <- Blocker[IO]
    transactor <- Database.transactor(config.database, fixedThreadPool, blocker)
    sttpBackend <- Resource.liftF(AsyncHttpClientCatsBackend[IO]())

    uwApi <- Resource.liftF(IO.pure(new UWApi(config.pythia.uwApi)))
    mailgunApi <- Resource.liftF(IO.pure(new MailgunWrapper(config.pythia.mailgunApi, config.pythia.senderDomain,
      EmailAddress(config.pythia.senderEmail, config.pythia.senderEmailName))))

    initDb <- Resource.liftF(IO.delay(Database.initialize(transactor)))

    scheduleNotifierController <- Resource.liftF {
      IO.pure {
        new ScheduleNotifierController(config.pythia.updatePeriodSeconds.seconds, uwApi, mailgunApi, transactor, sttpBackend)
      }
    }


    httpServer <- Resource.liftF {
      val baseRoutes: HttpRoutes[IO] = Base(scheduleNotifierController)
      val httpApp: HttpRoutes[IO] = Router(config.http.prefix -> baseRoutes)
      val svc = ApacheLogging(CORS(ErrorHandler(httpApp))).orNotFound
      val server = BlazeServerBuilder[IO].bindHttp(port = config.http.port, host = config.http.host)
        .withIdleTimeout(5.minutes)
        .withResponseHeaderTimeout(5.minutes)
        .withHttpApp(svc)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
        .map { c =>
          System.exit(c.code)
          c
        }
      IO.delay(server)
    }
  } yield ServerJobs(initDb, scheduleNotifierController, httpServer)


  def run(args: List[String]): IO[ExitCode] = {
    acquireConfigs.use { c =>
      c.initDb.start.flatMap { fiber =>
        fiber.join.flatMap(_ => c.scheduleNotifierController.run).unsafeRunAsyncAndForget()
        fiber.join.flatMap(_ => c.httpServer)
      }
    }
  }
}
