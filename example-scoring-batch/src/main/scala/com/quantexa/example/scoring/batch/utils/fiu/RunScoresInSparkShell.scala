/*
package com.quantexa.example.scoring.batch.utils.fiu

import org.apache.spark.sql.SparkSession
import com.quantexa.scoring.framework.spark.execution.SparkExecutor
import com.quantexa.example.scoring.batch.model.fiu.FiuBatchModelDefinition
import com.quantexa.example.scoring.scores.fiu.document.customer._
import com.quantexa.example.scoring.scores.fiu.entity._
import com.quantexa.example.scoring.scores.fiu.transaction._
import com.quantexa.example.scoring.model.fiu.FiuModelDefinition
import com.quantexa.scoring.framework.spark.execution.SparkScoringContext
import com.quantexa.scoring.framework.parameters.StubNoParameterService

object RunScoresInSparkShell {
  val spark: SparkSession = SparkSession
    .builder()
    .master("local")
    .appName("Quantexa Score")
    .getOrCreate()

  val batchFiuPhaseContext = SparkScoringContext(spark, FiuBatchModelDefinition, FiuBatchModelDefinition.parameterProvider, FiuBatchModelDefinition.dataDependencyProviderDefinitions)
  SparkExecutor.apply(FiuBatchModelDefinition.scores, batchFiuPhaseContext)

  val fiuPhaseContext = SparkScoringContext(spark, FiuModelDefinition, StubNoParameterService, FiuModelDefinition.dataDependencyProviderDefinitions)
  SparkExecutor.apply(FiuModelDefinition.scores, fiuPhaseContext)

  val pickTransactionHighToLowRiskCountry = fiuPhaseContext.model.scores.filter(st => Seq(TransactionFromHighToLowRiskCountry).contains(st.score))
  SparkExecutor.apply(pickTransactionHighToLowRiskCountry,fiuPhaseContext)

  val pickTransactionLowToHighRiskCountry = fiuPhaseContext.model.scores.filter(st => Seq(TransactionFromLowToHighRiskCountry).contains(st.score))
  SparkExecutor.apply(pickTransactionLowToHighRiskCountry,fiuPhaseContext)

  val pickTransactionToHighRiskCustomer = fiuPhaseContext.model.scores.filter(st => Seq(TransactionToHighRiskCustomer).contains(st.score))
  SparkExecutor.apply(pickTransactionToHighRiskCustomer,fiuPhaseContext)

  val pickUnusualOriginatingCountryForCustomer = fiuPhaseContext.model.scores.filter(st => Seq(UnusualCounterpartyCountryForCustomer).contains(st.score))
  SparkExecutor.apply(pickUnusualOriginatingCountryForCustomer,fiuPhaseContext)

  val pickHighTransactionVolume = fiuPhaseContext.model.scores.filter(st => Seq(UnusuallyHigh7DaysVolume).contains(st.score))
  SparkExecutor.apply(pickHighTransactionVolume,fiuPhaseContext)

  val pickHighValue = fiuPhaseContext.model.scores.filter(st => Seq(UnusuallyHigh7DaysValueForCustomer).contains(st.score))
  SparkExecutor.apply(pickHighValue,fiuPhaseContext)


  val docScores = fiuPhaseContext.model.scores.filter(st => Seq(HighRiskCustomer,NewCustomer,CancelledCustomer).contains(st.score))

  val pickHighRiskCustomer = fiuPhaseContext.model.scores.filter(st => Seq(HighRiskCustomer).contains(st.score))
  val pickNewCustomer = fiuPhaseContext.model.scores.filter(st => Seq(NewCustomer).contains(st.score))
  val pickCancelledCustomer = fiuPhaseContext.model.scores.filter(st => Seq(CancelledCustomer).contains(st.score))

  val pickCustomerFromHighRiskCountry = fiuPhaseContext.model.scores.filter(_.score.toString.contains("CustomerFromHighRiskCountry"))
  SparkExecutor.apply(pickCustomerFromHighRiskCountry,fiuPhaseContext)
  val pickNonReputableAddressCustomer = fiuPhaseContext.model.scores.filter(_.score.toString.contains("NonReputableAddressCustomer"))
  SparkExecutor.apply(pickNonReputableAddressCustomer,fiuPhaseContext)
//  val pickHighValueTransaction = fiuPhaseContext.model.scores.filter(st => Seq(HighValueTransaction).contains(st.score))

  val pickPhoneHit = fiuPhaseContext.model.scores.filter(st => Seq(PhoneHit).contains(st.score))
  val pickHighRiskCustomerIndividual = fiuPhaseContext.model.scores.filter(st => Seq(HighRiskCustomerIndividual).contains(st.score))
  val pickIndividualWithMultipleNationalIDs = fiuPhaseContext.model.scores.filter(st => Seq(IndividualWithMultipleNationalIDs).contains(st.score))
  val pickIndividualWithMultipleResidenceCountries = fiuPhaseContext.model.scores.filter(st => Seq(IndividualWithMultipleResidenceCountries).contains(st.score))
  val pickIndividualWithIDManipulation = fiuPhaseContext.model.scores.filter(st => Seq(IndividualWithIDManipulation).contains(st.score))


  val pickIndividualOnHotlist = fiuPhaseContext.model.scores.filter(st => Seq(IndividualOnHotlist).contains(st.score))
  val pickRecentlyJoinedBusiness = fiuPhaseContext.model.scores.filter(st => Seq(RecentlyJoinedBusiness).contains(st.score))
  val pickBusinessOnHotlistWithLinkAttribute = fiuPhaseContext.model.scores.filter(st => Seq(BusinessOnHotlistWithLinkAttribute).contains(st.score))

  SparkExecutor.apply(pickBusinessOnHotlistWithLinkAttribute,fiuPhaseContext)

}
*/