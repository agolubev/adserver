package com.comcast

import akka.actor.{ Actor, ActorLogging, Props }

final case class Campaign(partner_id: String, duration: Int, ad_content: String)
final case class Campaigns(campaigns: Seq[Campaign])

object AdRegistryActor {
  final case class ActionPerformed(success: Boolean, description: String)
  final case object Get
  final case class CreateCampaign(campaign: Campaign)
  final case class GetCampaign(partner_id: String)

  def props: Props = Props[AdRegistryActor]
}

class AdRegistryActor extends Actor with ActorLogging {
  import AdRegistryActor._

  var campaigns = Map.empty[String, (Campaign, Long)]

  def createCampaign(campaign: Campaign): Boolean = {
    campaigns.get(campaign.partner_id) match {
      case Some((_, time)) =>
        if (System.currentTimeMillis() > campaign.duration * 1000 + time) {
          campaigns += campaign.partner_id -> (campaign, System.currentTimeMillis())
          true
        } else false
      case None =>
        campaigns += campaign.partner_id -> (campaign, System.currentTimeMillis())
        true
    }
  }

  def receive: Receive = {
    case GetCampaign =>
      sender() ! Campaigns(campaigns.map { case (a, b) => b._1 }.toSeq)
    case CreateCampaign(campaign) =>
      sender() ! (
        if (createCampaign(campaign))
          ActionPerformed(true, s"Campaign ${campaign.ad_content} created.")
        else
          ActionPerformed(false, s"Cannot create another campaign as another one is active now")
      )
    case GetCampaign(partner_id) =>
      sender() ! campaigns.get(partner_id).map(_._1)
  }
}
