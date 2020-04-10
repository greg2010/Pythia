package org.kys.pythia

import cats.effect.{ContextShift, IO}
import com.typesafe.scalalogging.LazyLogging
import org.matthicks.mailgun.{EmailAddress, Mailgun, Message, MessageResponse}

class MailgunWrapper(apiKey: String, domain: String, senderEmail: EmailAddress) extends LazyLogging {
  private val mg = new Mailgun(domain, apiKey)

  private def send(message: Message)(implicit cs: ContextShift[IO]): IO[MessageResponse] = {
    IO.fromFuture(IO.delay(mg.send(message)))
  }

  private def toLogResponse(messageResponse: MessageResponse): String = {
    s"messageId=${messageResponse.id} messageResponse=${messageResponse.message}"
  }

  def sendGreeting(courseNumber: Int, friendlyName: String, email: String, currentStatus: Boolean)
                  (implicit cs: ContextShift[IO]): IO[MessageResponse] = {
    val msgSubject = s"$friendlyName subscription confirmation"
    val msgText =
      s"""Hello,
         |
         |You've subscribed to updates for course $friendlyName ($courseNumber).\n
         |The course has ${if (!currentStatus) "no "}spots available.
         |
         |Your Schedule Checker
         |""".stripMargin
    val msg = Message.simple(senderEmail, EmailAddress(email), msgSubject, msgText)

    this.send(msg).map { r =>
      logger.debug(s"Successfully sent subscribe confirmation to email=$email currentStatus=$currentStatus ${toLogResponse(r)}")
      r
    }
  }

  def sendStatusChange(courseNumber: Int, friendlyName: String, email: String, currentStatus: Boolean)
                      (implicit cs: ContextShift[IO]): IO[MessageResponse] = {
    val msgSubject = s"$friendlyName availability change"
    val msgText =
      s"""Hello,
         |
         |This email is to let you know that the availability for course $friendlyName ($courseNumber) has changed.
         |The course now has ${if (!currentStatus) "no "}spots available.
         |
         |Your Schedule Checker
         |""".stripMargin
    val msg = Message.simple(senderEmail, EmailAddress(email), msgSubject, msgText)

    this.send(msg).map { r =>
      logger.debug(s"Successfully sent status change to email=$email currentStatus=$currentStatus ${toLogResponse(r)}")
      r
    }
  }

  def sendUnsubscribe(courseNumber: Int, friendlyName: String, email: String)
                     (implicit cs: ContextShift[IO]): IO[MessageResponse] = {
    val msgSubject = s"$friendlyName subscription cancel confirmation"
    val msgText =
      s"""Hello,
         |
         |You've unsubscribed to updates for course $friendlyName ($courseNumber).\n
         |
         |Your Schedule Checker
         |""".stripMargin
    val msg = Message.simple(senderEmail, EmailAddress(email), msgSubject, msgText)

    this.send(msg).map { r =>
      logger.debug(s"Successfully sent unsubscribe confirmation to email=$email${toLogResponse(r)}")
      r
    }
  }
}
