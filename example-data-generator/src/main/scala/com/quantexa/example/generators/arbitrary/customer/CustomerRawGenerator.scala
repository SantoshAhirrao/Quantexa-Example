package com.quantexa.example.generators.arbitrary.customer

import java.sql.Date.valueOf

import com.quantexa.example.model.fiu.customer.CustomerRawModel.CustomerRaw
import com.quantexa.generators.model._
import com.quantexa.generators.templates.Generator
import com.quantexa.example.generators.config.GeneratorLoaderOptions
import com.quantexa.generators.utils.GeneratedEntities
import com.quantexa.generators.utils.GeneratorUtils._
import org.scalacheck.{Arbitrary, Gen}

class CustomerRawGenerator (generatorConfig: GeneratorLoaderOptions, generatedEntities: GeneratedEntities) extends Generator[CustomerRaw] {

  val customerStartDateRange = DateRange(valueOf("1990-01-01"), Some(valueOf("2009-12-26")))

  def getCustomerRaw : Seq[CustomerRaw] = {
    val arbitraryCustomer = (id: Long) => Arbitrary {
      val seed = new java.util.Random(generatorConfig.seed*id).nextLong
      for {
        cus_id_no <- id.option(1)
        genCountry = genCategory(generatorConfig.countryCodes, seed).option(1)
        businessFlag <- genCategory(generatorConfig.accountTypes, seed)
        cus_stat <- genCategory(generatorConfig.dataSourceCodes, seed).option(1)
        bus_registered_name <- pickRandomFromSequence(generatedEntities.businesses, seed).option(1)
        bus_registration_no <- randomIntInRange(1, 100000, seed).map(_.toString).option(1)
        bus_ctry_registration <- genCountry
        customerStartRange = genDateRangeBetween(customerStartDateRange, seed).sample.get
        cus_start_dt <- new java.sql.Timestamp(customerStartRange.from.getTime).option(1)
        cus_end_dt <- new java.sql.Timestamp(customerStartRange.to.get.getTime).option(1)
        personal_bus_flg <- businessFlag.option(1)
        gender <- genCategory(generatorConfig.possibleGenders, seed).option(1)
        genPerson: GeneratedPerson = pickRandomFromSequence(generatedEntities.names, seed)
        genForename = genPerson.forename
        initials <- genForename.charAt(0).toString.option(1)
        forename <- genForename.option(1)
        family_name <- genPerson.familyName.option(1)
        maiden_name <- genPerson.maidenName.option(0.2, seed)
        full_name <- genPerson.fullName.option(1)
        nationality <- genCountry
        cus_risk_rtng <- randomIntInRange(0, 10000, seed).map(_.toString).option(1)
        employee_flag <- randomBool(generatorConfig.isEmployee, seed).option(1).map(boolean => if (boolean.get) "t" else "f").option(1)
        residence_ctry <- genCountry
        consolidated_cust_address <- pickRandomFromSequence(generatedEntities.addresses, seed).fullAddress.option(1)
        personal_national_id_no <- randomLongInRange(10000000l, 99999999l, seed).map(_.toString).option (1)
        dt_of_birth <- new java.sql.Timestamp(genDoB(18, 99, seed).getTime).option(1)
      } yield {
        def isBusiness[T](field: Option[T]): Option[T] = if (businessFlag.contains("B")) {
          field
        } else {
          None
        }
        def isPerson[T](field: Option[T]): Option[T] = if (businessFlag.contains("P")) {
          field
        } else {
          None
        }
        val bus_registered_name_conditional = isBusiness(bus_registered_name)
        val bus_registration_no_conditional = isBusiness(bus_registration_no)
        val bus_ctry_registration_conditional = isBusiness(bus_ctry_registration)
        val gender_conditional = isPerson(gender)
        val initials_conditional = isPerson(initials)
        val forename_conditional = isPerson(forename)
        val family_name_conditional = isPerson(family_name)
        val maiden_name_conditional = isPerson(maiden_name)
        val full_name_conditional = isPerson(full_name)
        val nationality_conditional = isPerson(nationality)
        val cus_risk_rtng_conditional = isPerson(cus_risk_rtng)
        val employee_flag_conditional = isPerson(employee_flag)
        val personal_national_id_no_conditional = isPerson(personal_national_id_no)
        val dt_of_birth_conditional = isPerson(dt_of_birth)
        CustomerRaw(
          cus_id_no,
          cus_stat,
          bus_registered_name_conditional,
          bus_registration_no_conditional,
          bus_ctry_registration_conditional,
          cus_start_dt,
          cus_end_dt,
          personal_bus_flg,
          gender_conditional,
          initials_conditional,
          forename_conditional,
          family_name_conditional,
          maiden_name_conditional,
          full_name_conditional,
          nationality_conditional,
          cus_risk_rtng_conditional,
          employee_flag_conditional,
          residence_ctry,
          consolidated_cust_address,
          personal_national_id_no_conditional,
          dt_of_birth_conditional
        )
      }
    }
    randomWithIds[CustomerRaw](arbitraryCustomer, generatorConfig.numberOfCustomerDocuments, generatorConfig.seed)
  }

  override def generateRecords : Seq[CustomerRaw] = {
    getCustomerRaw
  }

}
