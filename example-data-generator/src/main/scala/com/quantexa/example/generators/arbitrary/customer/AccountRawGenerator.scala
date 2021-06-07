package com.quantexa.example.generators.arbitrary.customer

import java.util.Locale

import com.github.javafaker.Faker
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccountRaw
import com.quantexa.generators.templates.Generator
import com.quantexa.example.generators.config.GeneratorLoaderOptions
import com.quantexa.generators.utils.GeneratedEntities
import com.quantexa.generators.utils.GeneratorUtils._
import org.scalacheck.{Arbitrary, Gen}

class AccountRawGenerator(generatorConfig: GeneratorLoaderOptions, generatedEntities: GeneratedEntities) extends Generator[AccountRaw] {

  def getAccountRaw : Seq[AccountRaw] = {
    val arbitraryAccount = (id: Long) => Arbitrary {
      val seed = new java.util.Random(generatorConfig.seed*id).nextLong
      val faker = new Faker(new Locale("en-GB"), new java.util.Random(seed))
      for {
        prim_acc_no <- id.option(1)
        genAddress = pickRandomFromSequence(generatedEntities.addresses, seed)
        genCity = genAddress.city.option(1)
        personName = pickRandomFromSequence(generatedEntities.names, seed)
        companyName = pickRandomFromSequence(generatedEntities.businesses, seed)
        name = genCategory(Seq((0.5,companyName), (0.5, personName.fullName.toUpperCase)), seed)
        composite_acc_name <- name.option(1)
        consolidated_acc_address <- faker.address.fullAddress.option(1)
        acc_stat <- genCategory(generatorConfig.accountStates, seed).option(1)
        acc_district <- genCity
        rel_mngr_name <- faker.name.fullName.option(0.33, seed)
        raw_acc_name <- name.option(1)
        addr_line_one <- genAddress.address.option(1)
        city <- genCity
        state <- genAddress.state.option(1)
        postal_cde <- genAddress.postalCode.option(1)
        ctry <- genAddress.countryCode.option(1)
        acc_risk_rtng <- randomIntInRange(1,10000, seed).option(1)
        aml_alr_flg <- randomBool(generatorConfig.amlAlrFlagged, seed).option(1).map(boolean => if (boolean.get) "t" else "f").option(1)
        tel_no <- pickRandomFromSequence(generatedEntities.phoneNumbers, seed).option(0.85, seed)
      } yield AccountRaw(
        prim_acc_no,
        composite_acc_name,
        consolidated_acc_address,
        acc_stat,
        acc_district,
        rel_mngr_name,
        raw_acc_name,
        Some(""),
        Some(""),
        addr_line_one,
        city,
        state,
        postal_cde,
        ctry,
        acc_risk_rtng,
        aml_alr_flg,
        None,
        tel_no
      )
    }
    randomWithIds[AccountRaw](arbitraryAccount, generatorConfig.numberOfAccountDocuments, generatorConfig.seed)
  }

  override def generateRecords : Seq[AccountRaw] = {
    getAccountRaw
  }
}
