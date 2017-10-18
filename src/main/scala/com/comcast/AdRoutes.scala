package com.comcast

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{ get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.comcast.AdRegistryActor._

import scala.concurrent.Future
import scala.concurrent.duration._

trait AdRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[AdRoutes])

  def adRegistryActor: ActorRef

  implicit lazy val timeout = Timeout(5.seconds)
  lazy val adRoutes: Route =
    pathPrefix("ad") {
      concat(
        pathEnd {
          concat(
            get {
              //get all
              val campaigns: Future[Campaigns] = (adRegistryActor ? GetCampaign).mapTo[Campaigns]
              complete(campaigns)
            },
            post {
              //create
              entity(as[Campaign]) { campaign =>
                val campaignCreated: Future[ActionPerformed] =
                  (adRegistryActor ? CreateCampaign(campaign)).mapTo[ActionPerformed]
                onSuccess(campaignCreated) { performed =>
                  if (performed.success) {
                    log.info("Created campaign [{}]: {}", campaign.partner_id, performed.description)
                    complete((StatusCodes.Created, performed))
                  } else {
                    log.info("Failed to create campaign [{}]: {}", campaign.partner_id, performed.description)
                    complete((StatusCodes.Conflict, performed))

                  }
                }
              }
            }
          )
        },
        path(Segment) { name =>
          get {
            // get by partner id
            val maybeCampaign: Future[Option[Campaign]] =
              (adRegistryActor ? GetCampaign(name)).mapTo[Option[Campaign]]
            rejectEmptyResponse {
              complete(maybeCampaign)
            }
          }
        }
      )
    }

}
