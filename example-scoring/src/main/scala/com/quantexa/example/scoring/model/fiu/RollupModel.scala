package com.quantexa.example.scoring.model.fiu

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.quantexa.analytics.scala.scoring.model.ScoringModel.{KeyedBasicScoreOutput, KeyedScoreOutput, LookupKey, ScoreId}
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerDateKey, CustomerKey, TransactionKeys}
import com.quantexa.resolver.ingest.Model.id
import com.quantexa.scoring.framework.model.LookupModel.{KeyAnnotation, LookupSchema}

object RollupModel {

  //TODO: Try and move this JsonTypeInfo and SubTypes annotation to the CustomerRollup class. Previously attempted to use reflection for this but there was a problem with type tags
  /**
    * Below, we are stating that on any class that extends this trait, we can find a typeInformation field
    * The value of typeInformation is matched to a class in the JsonSubTypes array, this is how jackson knows
    * what type to use for deserializing when returning (JSON) data from  elastic.
    * */
  @JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "typeInformation"
  )
  @JsonSubTypes(
    Array(
      new Type(
        value = classOf[CustomerDateScoreOutput],
        name = ElasticTypeInformationFor.customerDateScoreOutput
      ),
      new Type(
        value = classOf[TransactionScoreOutput],
        name = ElasticTypeInformationFor.transactionScoreOutput
      )
    )
  )
  sealed trait RollupOutput extends Product

  /**Contains unique identifiers to ultimately identify the correlated case class. A small name is used to save space in spark and elastic.*/
  object ElasticTypeInformationFor{
    final val customerDateScoreOutput =  "CDSO"
    final val transactionScoreOutput =  "TSO"
  }

  case class CustomerDateScoreOutput(keys: CustomerDateKey,
                                     severity: Option[Int] = None,
                                     band: Option[String] = None,
                                     description: Option[String] = None,
                                     typeInformation: String = ElasticTypeInformationFor.customerDateScoreOutput) extends KeyedScoreOutput[CustomerDateKey] with RollupOutput {
    def toKeyedBasicScoreOutput = KeyedBasicScoreOutput(
      keyValues = Seq(
        LookupKey(name = "customerId", stringKey = Some(this.keys.customerId)),
        LookupKey(name = "analysisDate", dateKey = Some(this.keys.analysisDate))),
      docType = "transaction",
      severity = this.severity,
      band = this.band,
      description = this.description)
  }


  case class TransactionScoreOutput(keys: TransactionKeys,
                                    severity: Option[Int] = None,
                                    band: Option[String] = None,
                                    extraDescriptionText: Map[String, String] = Map.empty,
                                    description: Option[String] = None,
                                    typeInformation: String = ElasticTypeInformationFor.transactionScoreOutput) extends KeyedScoreOutput[TransactionKeys] with RollupOutput {
    def toKeyedBasicScoreOutput = KeyedBasicScoreOutput(
      keyValues = Seq(
        LookupKey(name = "transactionId", stringKey = Some(this.keys.transactionId))),
      docType = "transaction",
      severity = this.severity,
      band = this.band,
      description = this.description)
  }

  //Rollup Models (input to rollup document scores)
  case class CustomerRollup[T <: RollupOutput](@id @KeyAnnotation subject: String, keys: CustomerKey, customScoreOutputMap: collection.Map[ScoreId, Seq[T]]) extends LookupSchema

  //Rollup Models (output of rollup document scores)
  case class CustomerScoreOutputWithUnderlying(keys: CustomerKey,
                                               severity: Option[Int],
                                               band: Option[String],
                                               description: Option[String],
                                               underlyingScores: Seq[KeyedBasicScoreOutput]) extends KeyedScoreOutput[CustomerKey] {
    def toKeyedBasicScoreOutput = KeyedBasicScoreOutput(
      keyValues = Seq(LookupKey(name = "customerId", stringKey = Some(this.keys.customerId))),
      docType = "customer", severity = this.severity, band = this.band, description = this.description)
  }



}
