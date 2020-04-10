package org.kys.pythia

import java.util.concurrent.Executors

import doobie._
import doobie.implicits._
import cats.effect.{ContextShift, IO, Timer}
import com.typesafe.scalalogging.LazyLogging
import doobie.util.transactor.Transactor
import org.kys.pythia.db.models.CoursesWatchlistRow
import org.kys.pythia.uwapi.UWApi
import org.kys.pythia.uwapi.dto.course.Schedule
import sttp.client.SttpBackend
import sttp.client.asynchttpclient.WebSocketHandler

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class ScheduleNotifierController(period: FiniteDuration,
                                 UWApi: UWApi,
                                 mailgunWrapper: MailgunWrapper,
                                 tr: Transactor[IO],
                                 asyncHttpClientCatsBackend: SttpBackend[IO, Nothing, WebSocketHandler]) extends LazyLogging {
  private case class CourseStatus(course: CoursesWatchlistRow, hasChanged: Boolean, apiStatus: Boolean)

  private val ecCached = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private implicit val cs: ContextShift[IO] = IO.contextShift(ecCached)

  def getCourses: IO[List[CoursesWatchlistRow]] = {
    sql"select * from courses_watchlist"
      .query[CoursesWatchlistRow]
      .to[List]
      .transact(tr).map { res =>
      logger.debug(s"Got $res")
      res
    }
  }

  def getCourse(sectionId: Int, termId: Int, email: String): IO[Option[CoursesWatchlistRow]] = {
    sql"""
         |select * from courses_watchlist
         |where section_id = $sectionId and term_id = $termId and email = $email limit 1
          """.stripMargin.query[CoursesWatchlistRow].option.transact(tr).map { res =>
      // TODO log
      res
    }
  }

  def updateCourse(course: CoursesWatchlistRow, newStatus: Boolean): IO[Int] = {
    sql"update courses_watchlist set last_state_has_space = $newStatus where id = ${course.id}"
      .update.run.transact(tr).map { rows =>
      logger.debug(s"Updated ${course.id} with newStatus $newStatus")
      rows
    }
  }

  def insertCourse(sectionId: Int, termId: Int, email: String, isAvailable: Boolean, friendlyName: String): IO[Int] = {
    sql"""
         |insert into courses_watchlist (section_id, term_id, email, last_state_has_space, friendly_name) VALUES
         |($sectionId, $termId, $email, $isAvailable, $friendlyName)
         |""".stripMargin.update.run.transact(tr).map { rows =>
      logger.debug(s"Inserted $sectionId term $termId email $email status $isAvailable")
      rows
    }
  }

  def deleteCourse(sectionId: Int, termId: Int, email: String): IO[Int] = {
    sql"""
         |delete from courses_watchlist where
         |section_id = $sectionId and term_id = $termId and email = $email
         |""".stripMargin.update.run.transact(tr).map { rows =>
      logger.debug(s"Deleted ($sectionId $termId $email)")
      rows
    }
  }

  def createCourseRecord(sectionId: Int, termId: Int, email: String): IO[Unit] = {

    val q = UWApi.courses.getCourseSchedule(sectionId, termId)
    for {
      schedule <- asyncHttpClientCatsBackend.send(q).flatMap(r => IO.fromEither(r.body))
      insert <- this.insertCourse(sectionId, termId, email, isAvailable(schedule), friendlyName(schedule))
      notify <- mailgunWrapper.sendGreeting(sectionId, friendlyName(schedule), email, isAvailable(schedule))
    } yield ()
  }

  def deleteCourseRecord(sectionId: Int, termId: Int, email: String): IO[Unit] = {
    for {
      courseRecord <- this.getCourse(sectionId, termId, email).map(_.get) //TODO better option handling
      delete <- this.deleteCourse(sectionId, termId, email)
      notify <- mailgunWrapper.sendUnsubscribe(sectionId, courseRecord.friendlyName, email)
    } yield ()
  }

  def isAvailable(schedule: Schedule): Boolean = schedule.enrollment_capacity > schedule.enrollment_total
  def friendlyName(schedule: Schedule): String =
    s"${schedule.subject} ${schedule.catalog_number} - ${schedule.title} - ${schedule.section}"

  private def getCronRunnable(implicit timer: Timer[IO], cs: ContextShift[IO]): IO[Unit] = {
    import cats.implicits._


    val task = for {
      courses <- this.getCourses
      available <- {
        courses.map { course =>
          val q = UWApi.courses.getCourseSchedule(course.sectionId, course.termId)
          val available = asyncHttpClientCatsBackend.send(q).flatMap(r => IO.fromEither(r.body)).map(isAvailable)
          available.map { apiStatus =>
            logger.debug(s"Processing ${course.sectionId} lastStatus ${course.lastStateHasSpace} newStatus $apiStatus")
            CourseStatus(course, apiStatus != course.lastStateHasSpace, apiStatus)
          }
        }.sequence
      }
      changed <- IO.pure(available.filter(_.hasChanged))
      update <- changed.map(c => this.updateCourse(c.course, c.apiStatus)).sequence
      notify <- changed.map { c =>
        mailgunWrapper.sendStatusChange(c.course.sectionId, c.course.friendlyName, c.course.email, c.apiStatus)
      }.sequence
    } yield notify

    task >>
      IO.pure(logger.debug(s"Finished processing cron task, sleeping for $period")) >>
      IO.sleep(period) >>
      this.getCronRunnable
  }

  def run: IO[Unit] = {
    val ecFixed = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
    implicit val timer: Timer[IO] = IO.timer(ecFixed)

    this.getCronRunnable
  }
}
