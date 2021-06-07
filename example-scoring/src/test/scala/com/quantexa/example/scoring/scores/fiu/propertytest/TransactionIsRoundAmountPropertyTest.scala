package com.quantexa.example.scoring.scores.fiu.propertytest

import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testScoreableCustomer
import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.{testCustomerDateFacts, testTransactionFacts}
import com.quantexa.example.scoring.scores.fiu.transaction.TransactionIsRoundAmount
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers

class TransactionIsRoundAmountPropertyTest extends FlatSpec with Checkers{

  implicit val scoreInput = ScoreInput.empty

  "TransactionIsRoundAmountPropertyTest" should "score if amountOrigCurrency is a multiple of 100" in {
    //Note that round amount was defined as multiples of 100 so project example data would trigger it (not realistic in real life).
    val roundAmountGen =  Gen.chooseNum(0d, 1000000d) suchThat (_ % 100 == 0)

    val property = forAll(roundAmountGen){amount =>
      TransactionIsRoundAmount.score(transaction = testTransactionFacts.copy(amountOrigCurrency = amount)).flatMap(_.severity).isDefined
    }
    check(property)
  }

  it should "not score if amountOrigCurrency is not a multiple of 100" in {
    //Note that round amount was defined as multiples of 100 so project example data would trigger it (not realistic in real life).
    val roundAmountGen =  Gen.chooseNum(0d, 1000000d) suchThat (_ % 100 != 0)

    val property = forAll(roundAmountGen){amount =>
      TransactionIsRoundAmount.score(transaction = testTransactionFacts.copy(amountOrigCurrency = amount)).isEmpty
    }
    check(property)
  }

  def transactionFactsGen(amountOrigCurrencyChoices: Seq[Double]): Gen[TransactionFacts] = for{
    transactionId <- Gen.numStr
    customerId <- Gen.numStr
    customerName <- Gen.alphaStr
    postingDayDateDay <- Gen.chooseNum(1, 31)
    postingDayDateMonth <- Gen.chooseNum(1, 12)
    postingDayDateYear <- Gen.chooseNum(2016, 2017)
    analysisAmount <- Gen.chooseNum(0d,1000000d)
    amountOrigCurrency <- Gen.oneOf(amountOrigCurrencyChoices)
    origCurrency <- Gen.oneOf(currencies)
    customerOriginatorOrBeneficiary <- Gen.oneOf("originator", "beneficiary")
    customerAccountCountry <- Gen.option(Gen.oneOf("UK", "USA", "DE"))
    customerAccountCountryCorruptionIndex <- Gen.option(Gen.chooseNum(0,100))
    counterPartyAccountCountry <- Gen.option(Gen.oneOf("UK", "USA", "DE"))
    counterPartyAccountCountryCorruptionIndex <- Gen.option(Gen.chooseNum(0,100))
  } yield TransactionFacts(
    transactionId = transactionId,
    customerId = customerId,
    customerName = customerName,
    analysisDate = convertToSqlDate(postingDayDateDay, postingDayDateMonth, postingDayDateYear).get,
    analysisAmount = analysisAmount,
    amountOrigCurrency = amountOrigCurrency,
    origCurrency = origCurrency,
    customerOriginatorOrBeneficiary = customerOriginatorOrBeneficiary,
    customerAccountCountry = customerAccountCountry,
    customerAccountCountryCorruptionIndex = customerAccountCountryCorruptionIndex,
    counterPartyAccountCountry = counterPartyAccountCountry,
    counterPartyAccountCountryCorruptionIndex = counterPartyAccountCountryCorruptionIndex,
    customer = testScoreableCustomer,
    customerDateFacts = Some(testCustomerDateFacts),
    paymentToCustomersOwnAccountAtBank = None
  )

  it should "not change score result if other fields change" in {

    val amountsForTest = Seq(1000d, 1001d)
    val property = forAll(transactionFactsGen(amountsForTest)){trans =>
      val testAmount = trans.amountOrigCurrency
       testAmount match {
         case 1000 => TransactionIsRoundAmount.score(transaction = trans).flatMap(_.severity).isDefined
         case 1001 => TransactionIsRoundAmount.score(transaction = trans).isEmpty
      }
    }
    check(property)
  }

}