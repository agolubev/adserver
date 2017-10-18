package com.comcast

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }
import spray.json._

class AdRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with AdRoutes {

  override val adRegistryActor: ActorRef =
    system.actorOf(AdRegistryActor.props, "adRegistry")

  lazy val routes = Route.seal(adRoutes)

  val testCampaign = Campaign("LLC", 1, "My ad")

  "AdRoutes" should {
    "return no campaigns if no present (GET /ad)" in {
      val request = HttpRequest(uri = "/ad")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"campaigns":[]}""")
      }
    }

    "be able to add campaign (POST /ad)" in {
      val campaignEntity = Marshal(testCampaign).to[MessageEntity].futureValue

      val request = Post("/ad").withEntity(campaignEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"success":true,"description":"Campaign My ad created."}""")
      }
    }

    "return error in case campaign is still run (POST /ad)" in {
      val request2 = Post("/ad").withEntity(Marshal(Campaign("LLC", 2, "My ad2")).to[MessageEntity].futureValue)

      request2 ~> routes ~> check {
        status should ===(StatusCodes.Conflict)
      }
    }

    "return campaign for one (GET /ad/LLC)" in {
      val request = HttpRequest(uri = "/ad/LLC")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(testCampaign.toJson.toString())
      }
    }

    "return 404 in no compaign (GET /ad/LLC2)" in {
      val request = HttpRequest(uri = "/ad/LLC2")

      request ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

  }

}
