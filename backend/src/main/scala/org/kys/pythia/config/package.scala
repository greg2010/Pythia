package org.kys.pythia

import cats.effect.{Blocker, ContextShift, IO, Resource}
import com.typesafe.scalalogging.LazyLogging
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

package object config extends LazyLogging {

  def load(implicit cs: ContextShift[IO]): Resource[IO, RootConfig] = {
    Blocker[IO].flatMap { _ =>
      Resource.liftF(ConfigSource.default.loadF[IO, RootConfig])
    }
  }
}
