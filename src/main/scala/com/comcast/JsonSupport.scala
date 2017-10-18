package com.comcast

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.comcast.AdRegistryActor.ActionPerformed
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit val adJsonFormat = jsonFormat3(Campaign)
  implicit val adsJsonFormat = jsonFormat1(Campaigns)

  implicit val actionPerformedJsonFormat = jsonFormat2(ActionPerformed)
}
