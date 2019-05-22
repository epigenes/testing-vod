package models

import play.api.libs.json.Json

case class MyFile(name: String)

object MyFile {

  implicit val writes = Json.writes[MyFile]
}