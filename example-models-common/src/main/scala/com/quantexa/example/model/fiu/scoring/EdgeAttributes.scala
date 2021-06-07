package com.quantexa.example.model.fiu.scoring

import com.quantexa.scoring.framework.model.ScoreDefinition.Model.EdgeAttributes

object EdgeAttributes {

  case class BusinessEdge(total_records: Option[Int],
                          businessOnHotlist: Option[String]
                         ) extends EdgeAttributes

  case class IndividualEdge(
                             total_records: Option[Int],
                             individualOnHotlist: Option[String]
                           ) extends EdgeAttributes

}
