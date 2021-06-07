package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.CustomerDateFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.CustomerDateScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier, ScoreParameters}

trait UnusuallyHighVolume extends AugmentedDocumentScore[CustomerDateFacts,CustomerDateScoreOutput]
    with ScoreParameters {

  def id: String
  def name: String

  /** The period of time to aggregate transaction volume over */
  def period: Int

  /** The current volume of transactions over the period specified */
  def currentVolume(customerDateFact: CustomerDateFacts): Long

  /** The historical average of value over the period specified */
  def historicalAverage(customerDateFact: CustomerDateFacts): Double

  /** The historical standard deviation of observations over the period specified */
  def historicalStDev(customerDateFact: CustomerDateFacts): Double

  def scoreDescription(volume: Long, historicalAverage: Double, historicalStDev: Double, customerName: Option[String], customerId: String): String = {

      val lowerBound = math.max(historicalAverage - 2 * historicalStDev,0).toLong
      val upperBound = (historicalAverage + 2 * historicalStDev).toLong

      customerName match {
        case Some(nameValue) => s"Over the previous $period $dayGrammar, customer $nameValue ($customerId) was involved in $volume transactions which is unusually high compared to the average range ($lowerBound - $upperBound)."
        case _ => s"Over the previous $period $dayGrammar, customer ($customerId) was involved in $volume transactions which is unusually high compared to the average range ($lowerBound - $upperBound)."

      }

  }

  val parameters: Set[ScoreParameterIdentifier] = Set(
    ParameterIdentifier(None, s"${id}_numberOfStdDevsUnusualMediumSeverity"),
    ParameterIdentifier(None, s"${id}_numberOfStdDevsUnusualHighSeverity")
  )

  def score(customerDate: CustomerDateFacts)(implicit scoreInput: ScoreInput): Option[CustomerDateScoreOutput] = {

      val medThreshold = parameter[Double](s"${id}_numberOfStdDevsUnusualMediumSeverity")
      val highThreshold = parameter[Double](s"${id}_numberOfStdDevsUnusualHighSeverity")


      val (severity, band) = getSeverityAndBand(currentVolume(customerDate), historicalAverage(customerDate), historicalStDev(customerDate), medThreshold, highThreshold)

    if (severity.isDefined) {
      Some(CustomerDateScoreOutput(
        keys = customerDate.key,
        severity,
        band,
        description = Some(scoreDescription(currentVolume(customerDate), historicalAverage(customerDate), historicalStDev(customerDate), customerDate.customerName, customerDate.key.customerId))))
    } else None


  }


  private val dayGrammar = if (period == 1) "day" else "days"

  private def getSeverityAndBand(volume: Double, historicalAverage: Double, historicalStDev: Double,
                            medThreshold: Double, highThreshold: Double): (Option[Int], Option[String]) = {
    if (historicalAverage > 0 && historicalStDev > 0) {
        volume match {
          case vol if vol > (historicalAverage + highThreshold * historicalStDev) => (Some(100), Some("high"))
          case vol if vol > (historicalAverage + medThreshold * historicalStDev) => (Some(50), Some("medium"))
          case _ => (None, None)
    }} else (None, None)
  }
}

  object UnusuallyHigh1DayVolume extends UnusuallyHighVolume {
    def id = "UnusuallyHigh1DayTransactionVolume"
    def name = "Unusually high 1 day transaction volume"
    def period = 1

    def currentVolume(customerDateFact: CustomerDateFacts): Long = customerDateFact.numberOfTransactionsToday

    def historicalAverage(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousAverageTransactionVolumeToDate.getOrElse(0D)

    def historicalStDev(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousStdDevTransactionVolumeToDate.getOrElse(0D)
  }
  object UnusuallyHigh7DaysVolume extends UnusuallyHighVolume {
    def id = "UnusuallyHigh7DaysTransactionVolume"
    def name = "Unusually high 7 days transaction volume"
    def period = 7

    def currentVolume(customerDateFact: CustomerDateFacts): Long = customerDateFact.numberOfTransactionsOverLast7Days

    def historicalAverage(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousAverageTransactionVolumeLast7Days.getOrElse(0D)

    def historicalStDev(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousStdDevTransactionVolumeLast7Days.getOrElse(0D)
  }
  object UnusuallyHigh30DaysVolume extends UnusuallyHighVolume {
    def id = "UnusuallyHigh30DaysTransactionVolume"
    def name = "Unusually high 30 days transaction volume"
    def period = 30

    def currentVolume(customerDateFact: CustomerDateFacts): Long = customerDateFact.numberOfTransactionsOverLast30Days

    def historicalAverage(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousAverageTransactionVolumeLast30Days.getOrElse(0D)

    def historicalStDev(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousStdDevTransactionVolumeLast30Days.getOrElse(0D)
  }







