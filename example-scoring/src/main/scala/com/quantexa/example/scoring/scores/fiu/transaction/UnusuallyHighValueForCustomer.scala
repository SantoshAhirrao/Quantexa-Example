package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.CustomerDateFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.CustomerDateScoreOutput
import com.quantexa.example.scoring.model.fiu.ScoringModel._
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ParameterIdentifier, ScoreParameters}

trait UnusuallyHighValueForCustomer extends AugmentedDocumentScore[CustomerDateFacts, CustomerDateScoreOutput]
  with ScoreParameters {

  def id: String

  def name: String

  /** The period of time to aggregate volume activity over */
  def period: Int

  /** The current amount in value over the period specified*/
  def currentAmount(customerDateFact: CustomerDateFacts): Double

  /** The historical average of value over the period specified  */
  def historicalAverage(customerDateFact: CustomerDateFacts): Double

  /** The historical standard deviation of observations over the period specified */
  def historicalStDev(customerDateFact: CustomerDateFacts): Double

  def scoreDescription(value: Double, historicalAverage: Double, historicalStDev: Double, customerName: Option[String], customerId: String): String = {

    val lowerBound = math.max(historicalAverage - 2 * historicalStDev,0).round
    val upperBound = (historicalAverage + 2 * historicalStDev).round

    customerName match {
      case Some(nameValue) => s"Over the previous $period $dayGrammar, customer $nameValue ($customerId) made transactions with a total value of $value which is unusually high compared to the average range ($lowerBound - $upperBound)."
      case _ => s"Over the previous $period $dayGrammar, customer ($customerId) made transactions with a total value of $value which is unusually high compared to the average range ($lowerBound - $upperBound)."
      }

    }

  val parameters: Set[ScoreParameterIdentifier] = Set(
    ParameterIdentifier(None, s"${id}_numberOfStdDevsUnusualMediumSeverity"),
    ParameterIdentifier(None, s"${id}_numberOfStdDevsUnusualHighSeverity")
  )

  def score(customerDate: CustomerDateFacts)(implicit scoreInput: ScoreInput): Option[CustomerDateScoreOutput] = {

    val medThreshold = parameter[Double](s"${id}_numberOfStdDevsUnusualMediumSeverity")
    val highThreshold = parameter[Double](s"${id}_numberOfStdDevsUnusualHighSeverity")


    val (severity, band) = getSeverityAndBand(currentAmount(customerDate), historicalAverage(customerDate), historicalStDev(customerDate), medThreshold, highThreshold)

    if (severity.isDefined) {
      Some(CustomerDateScoreOutput(
        keys = CustomerDateKey(customerDate.key.customerId, customerDate.key.analysisDate),
        severity,
        band,
        description = Some(scoreDescription(currentAmount(customerDate), historicalAverage(customerDate), historicalStDev(customerDate), customerDate.customerName, customerDate.key.customerId))))
    } else None
  }

  private val dayGrammar = if (period == 1) "day" else "days"

  private def getSeverityAndBand(customerTransactionDateRangeAmount: Double, historicalAverage: Double, historicalStDev: Double,
                                 medThreshold: Double, highThreshold: Double): (Option[Int], Option[String]) = {
    if (historicalAverage > 0 && historicalStDev > 0) {
      customerTransactionDateRangeAmount match {
        case amount if amount > (historicalAverage + highThreshold * historicalStDev) => (Some(100), Some("high"))
        case amount if amount > (historicalAverage + medThreshold * historicalStDev) => (Some(50), Some("medium"))
        case _ => (None, None)
      }
    } else (None, None)
  }
}

object UnusuallyHigh1DayValueForCustomer extends UnusuallyHighValueForCustomer {
  def id = "UnusuallyHigh1DayValueForCustomer"

  def name = "Unusually high value of transaction activity over 1 day for customer"

  def period = 1

  def currentAmount(customerDateFact: CustomerDateFacts): Double = customerDateFact.totalValueTransactionsToday

  def historicalAverage(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousAverageTransactionAmountToDate.getOrElse(0D)

  def historicalStDev(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousStdDevTransactionAmountToDate.getOrElse(0D)

}

object UnusuallyHigh7DaysValueForCustomer extends UnusuallyHighValueForCustomer {
  def id = "UnusuallyHigh7DaysValueForCustomer"

  def name = "Unusually high value of transaction activity over 7 days for customer"

  def period = 7

  def currentAmount(customerDateFact: CustomerDateFacts): Double = customerDateFact.totalValueTransactionsOverLast7Days

  def historicalAverage(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousAverageTransactionAmountLast7Days.getOrElse(0D)

  def historicalStDev(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousStdDevTransactionAmountLast7Days.getOrElse(0D)
}

object UnusuallyHigh30DaysValueForCustomer extends UnusuallyHighValueForCustomer {
  def id = "UnusuallyHigh30DaysValueForCustomer"

  def name = "Unusually high value of transaction activity over 30 days for customer"

  def period = 30

  def currentAmount(customerDateFact: CustomerDateFacts): Double = customerDateFact.totalValueTransactionsOverLast30Days

  def historicalAverage(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousAverageTransactionAmountLast30Days.getOrElse(0D)

  def historicalStDev(customerDateFact: CustomerDateFacts): Double = customerDateFact.previousStdDevTransactionAmountLast30Days.getOrElse(0D)
}

