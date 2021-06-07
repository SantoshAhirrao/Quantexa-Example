package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.model.fiu.customer.CustomerModel.Account
import com.quantexa.example.model.fiu.transaction.TransactionModel.TransactionAccount
import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import org.scalacheck.Gen

import scala.reflect.ClassTag
case class TransactionAccountGenerator(accounts: Seq[Account]) extends GeneratesDataset[TransactionAccount] {

   def generate(implicit ct: ClassTag[TransactionAccount]): Gen[TransactionAccount] = {
    val generator: Gen[TransactionAccount] = for {
      account <- Gen.oneOf(accounts)
      accountName = account.rawAccountName
      accountIban <- Gen.some(ibanGen)
      accountCurrency <- Gen.option(Gen.oneOf(currencies))
      accountCountryISO3Code <- Gen.option(countryIsoGen)
      accountCountry <- Gen.option(countryGen)
      transaccountId = account.primaryAccountNumber.toString
      productType <- Gen.option(Gen.oneOf(productTypes))
    } yield
      TransactionAccount(
        transaccountId
        , transaccountId
        , accountName
        , None
        , accountIban = accountIban
        , accountCurrency = accountCurrency
        , accountCountryISO3Code = accountCountryISO3Code
        , accountCountry = accountCountry
        , productType = productType
      )
    generator
  }
}
