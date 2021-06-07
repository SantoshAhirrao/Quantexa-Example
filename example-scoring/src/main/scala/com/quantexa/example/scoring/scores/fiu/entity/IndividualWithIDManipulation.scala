package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, EntityRollUpScore, HitAggregation}
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer

/**This rule detects signs of ID Manipulation against an individual, by detecting where an entity has multiple features which should be distinct to an individual.
  * In particular, it looks at multiple dates of birth, national identifiers, and names.*/
object IndividualWithIDManipulation extends EntityRollUpScore[Individual,Customer,BasicScoreOutput]
  with HitAggregation {

  val id = "IndividualWithIDManipulation"

  def score(ind: Individual, customerDocs: Seq[Customer])(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {
    val datesOfBirth = customerDocs.flatMap(_.dateOfBirth).distinct
    val nationalIdNos = customerDocs.flatMap(_.personalNationalIdNumber).distinct
    val names = customerDocs.flatMap(_.parsedCustomerName).flatMap(_.nameDisplay).distinct

    val multipleIdentifiersFound = !(datesOfBirth.size <= 1 && nationalIdNos.size <= 1 && names.size <= 1)

    if (multipleIdentifiersFound) {
      Some(
          BasicScoreOutput(
             description = s"""${ind.fullName} may be manipulating their identity \r \n
                    |Names: ${names.mkString(",")}
                    |Dates of birth: ${datesOfBirth.mkString(",")} \r \n
                    |National Identifiers: ${nationalIdNos.mkString(",")} \r \n
                    """.stripMargin,
             severity = 100
          )
      )
    } else None
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}