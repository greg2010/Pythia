package org.kys.pythia.http.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.Effect
import com.typesafe.scalalogging.LazyLogging
import org.http4s.dsl.Http4sDsl
import org.http4s._
import cats.implicits._


/**
  * Error handling middleware for [[org.http4s]] REST server. It converts various exceptions into http status codes,
  * and returns 500 on unknown exceptions
  */
object ErrorHandler extends LazyLogging {

  def apply[F[_]](service: HttpRoutes[F])(implicit F: Effect[F]): HttpRoutes[F] = {
    Kleisli { req: Request[F] =>
      val dslInstance = Http4sDsl.apply[F]
      import dslInstance._
      import org.http4s.circe.CirceEntityEncoder._
      OptionT {
        service(req).value.attempt.flatMap {
          // Case request is valid and response is generated
          case Right(Some(resp)) => F.pure(resp) // Case no response is generated (404)
          case Right(None) => Response.notFoundFor(req) // Case non-200 response is generated (parsing exception, etc)
          case Left(ex: MessageFailure) =>
            F.pure(ex.toHttpResponse[F](req.httpVersion)) // All other exceptions
          case Left(ex) => logger.error("Caught unknown exception", ex)
            InternalServerError("Error occurred")
        }.map { resp: Response[F] =>
          Some(resp)
        }
      }
    }
  }
}
