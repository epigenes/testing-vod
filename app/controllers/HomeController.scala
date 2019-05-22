package controllers

import java.io.File
import java.util.UUID

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import javax.inject._
import models.{CompleteMessage, IngestMessage, PlainMessage, Workflow}
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc._

import scala.collection.mutable

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  val AWS_ACCESS_KEY = "AKIA2BGEHHRP42HST3IB"
  val AWS_SECRET_KEY = "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"

  val region = "eu-west-2"

  val awsCreds = new AWSCredentialsProvider {
    override def getCredentials: AWSCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
    override def refresh(): Unit = ()
  }

  val awsConfig = new ClientConfiguration()
  awsConfig.setSignerOverride("AWSS3V4SignerType")
  val awsClient = AmazonS3ClientBuilder
    .standard()
    .withCredentials(awsCreds)
    .withClientConfiguration(awsConfig)

  awsClient.setRegion(region)

  def index() = Action { implicit request =>

    Ok(views.html.index())
  }

  def videos() = Action { implicit request =>

    Ok(views.html.videos(map))
  }

  def upload() = Action(parse.multipartFormData) { implicit request =>

    val bucketName = "test-vod-source-e882la6amejr"

    request.body.file("uploadFile").fold(BadRequest("No file found")) { success =>

        val fileToUpload = new File(success.ref.path.toUri)

        try {
          val uniqueFileName = UUID.randomUUID() + success.filename

          awsClient.build().putObject(bucketName, uniqueFileName, fileToUpload)

          println("-" * 50)
          println(s"video name: $uniqueFileName")
          println("-" * 50)

          Redirect(routes.HomeController.videos())
        } catch {
          case e: Exception => InternalServerError(s"AWS fucked up with message: ${e.getMessage}")
        }
      }
  }

  private val map: mutable.Map[String, Workflow] = mutable.Map[String, Workflow]()

  def notification() = Action { implicit request =>

    println("*" * 50)
    println("Notification received")
    println(request.body)
    println("*" * 50)

    request.body.asText.map(x => Json.fromJson[Workflow](Json.parse(x))) match {
      case Some(JsSuccess(workflow@Workflow(_, _, _, message: CompleteMessage, _, _), _)) => map += message.srcVideo -> workflow
      case Some(JsSuccess(workflow@Workflow(_, _, _, message: IngestMessage, _, _), _))   => map += message.srcVideo -> workflow
      case _                                                                              => ()
    }

    Ok("Consider me notified buddy")
  }

  def poll(srcVideo: String) = Action { implicit request =>

    map.get(srcVideo) match {
      case Some(Workflow(_, _, _, _: CompleteMessage, _, _)) =>
        Ok("Workflow complete")
      case Some(_) => NotFound("Workflow not complete")
      case None    => NotFound("No update available")
    }
  }
}
