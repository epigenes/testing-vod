package models

import cats.implicits._
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import java.util.UUID

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.Try

case class Workflow(workflowType: WorkflowType, messageId: UUID, topicArn: String, message: Message, timestamp: LocalDateTime, subscribeURL: Option[String])

object Workflow {

  implicit val reads: Reads[Workflow] = (json: JsValue) => {
    ((json \ "Type").validate[WorkflowType] and
    (json \ "MessageId").validate[UUID] and
    (json \ "TopicArn").validate[String] and
    (json \ "Message").validate[Message] and
    (json \ "Timestamp").validate[LocalDateTime] and
    (json \ "SubscribeURL").validateOpt[String])(Workflow.apply(_, _, _, _, _, _))
  }
}

sealed trait WorkflowType
case object Notification extends WorkflowType
case object SubscriptionConfirmation extends WorkflowType

object WorkflowType {

  implicit val reads: Reads[WorkflowType] = {
    case JsString("Notification") => JsSuccess(Notification)
    case JsString("SubscriptionConfirmation") => JsSuccess(SubscriptionConfirmation)
    case json => JsError(s"Unable to parse workflow type from: $json")
  }
}

sealed trait Status
case object Ingest extends Status
case object Complete extends Status

object Status {

  implicit val reads: Reads[Status] = {
    case JsString("Complete") => JsSuccess(Complete)
    case JsString("Ingest") => JsSuccess(Ingest)
    case json => JsError(s"Unable to parse status from json: $json")
  }
}

sealed trait Message

case class IngestMessage(status: Status, guid: String, srcVideo: String) extends Message

object IngestMessage {

  implicit val reads: Reads[IngestMessage] = Json.reads[IngestMessage]
}

case class CompleteMessage(
                            workflowStatus: Status,
                            frameCapture: Boolean,
                            workflowName: String,
                            workflowTrigger: String,
                            encodingProfile: Int,
                            cloudFront: String,
                            archiveSource: Boolean,
                            startTime: LocalDateTime,
                            jobTemplate: String,
                            srcVideo: String,
                            srcBucket: String,
                            srcHeight: Int,
                            srcWidth: Int,
                            EndTime: LocalDateTime,
                            mp4Outputs: List[String],
                            mp4Urls: List[String],
                            hlsPlaylist: String,
                            hlsUrl: String,
                            dashPlaylist: String,
                            dashUrl: String,
                            guid: UUID
                          ) extends Message

object CompleteMessage {

  implicit val dateTimeReads: Reads[LocalDateTime] = new Reads[LocalDateTime] {
    override def reads(json: JsValue): JsResult[LocalDateTime] = json match {
      case JsString(dateTime) =>
        Try(LocalDateTime.parse(dateTime.split("\\.").head, ofPattern("yyyy-MM-dd HH:mm"))) // Doesn't include seconds so is essentially useless but lambda notification service uses fractions of a second which I can't get to work so fuck it
          .fold(
            error => JsError(s"Unable to parse date time ($json) with error $error"),
            JsSuccess(_)
          )
      case _ => JsError(s"Unable to parse date time from: $json")
    }

  }

  implicit val reads: Reads[CompleteMessage] = Json.reads[CompleteMessage]
}

case class PlainMessage(value: String) extends Message

object Message {

  implicit val reads: Reads[Message] = (json: JsValue) =>
    json match {
      case JsString(value) => Try(Json.fromJson[Message](Json.parse(value))(reads)).getOrElse(JsSuccess(PlainMessage(value)))
      case _               => json.validate[CompleteMessage] orElse json.validate[IngestMessage]
    }
}