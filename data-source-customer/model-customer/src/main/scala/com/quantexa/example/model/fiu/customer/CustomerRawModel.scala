package com.quantexa.example.model.fiu.customer

import com.quantexa.etl.validation.row.ValidationOps._
import cats.implicits._
import com.quantexa.etl.validation.row.{DatasourceValidator, ValidationError, ValidationResult}

/***
  * Raw case class models used to represent the flat input raw CSV files as they are read to parquet
  */

object CustomerRawModel {

  case class CustomerRaw (
                           cus_id_no: Option[Long],
                           cus_stat: Option[String],
                           bus_registered_name: Option[String],
                           bus_registration_no: Option[String],
                           bus_ctry_registration: Option[String],
                           cus_start_dt: Option[java.sql.Timestamp],
                           cus_end_dt: Option[java.sql.Timestamp],
                           personal_bus_flg: Option[String],
                           gender: Option[String],
                           initials: Option[String],
                           forename: Option[String],
                           family_name: Option[String],
                           maiden_name: Option[String],
                           full_name: Option[String],
                           nationality: Option[String],
                           cus_risk_rtng: Option[String],
                           employee_flag: Option[String],
                           residence_ctry: Option[String],
                           consolidated_cust_address: Option[String],
                           personal_national_id_no: Option[String],
                           dt_of_birth: Option[java.sql.Timestamp]
                         )

  case class AccountRaw (
                          prim_acc_no: Option[Long],
                          composite_acc_name: Option[String],
                          consolidated_acc_address: Option[String],
                          acc_stat: Option[String],
                          acc_district: Option[String],
                          rel_mngr_name: Option[String],
                          raw_acc_name: Option[String],
                          acc_opened_dt: Option[String],
                          acc_closed_dt: Option[String],
                          addr_line_one: Option[String],
                          city: Option[String],
                          state: Option[String],
                          postal_cde: Option[String],
                          ctry: Option[String],
                          acc_risk_rtng: Option[Int],
                          aml_alr_flg: Option[String],
                          sar_count: Option[String],
                          tel_no: Option[String]
                        )

  case class AccToCusRaw (
                           cus_id_no: Option[Long],
                           prim_acc_no: Option[Long],
                           acc_opened_dt: Option[java.sql.Timestamp],
                           acc_closed_dt: Option[java.sql.Timestamp],
                           acc_link_created_dt: Option[java.sql.Timestamp],
                           acc_link_end_dt: Option[java.sql.Timestamp],
                           data_source_cde: Option[String],
                           data_source_ctry: Option[String],
                           created_dt: Option[java.sql.Timestamp]
                         )

  object CustomerRaw {

    //For custom logic, validation rules can either be written as below:
    val customerDatesRule: CustomerRaw => ValidationResult[CustomerRaw] = customerRaw => {
        val customerStartDate = customerRaw.cus_start_dt
        val customerEndDate = customerRaw.cus_end_dt

        if(customerStartDate.isDefined && customerEndDate.isDefined){
          if(customerEndDate.get.after(customerStartDate.get)) customerRaw.validNel
          else ValidationError("CustomerDatesRule", "cus_start_dt, cus_end_dt", "Customer end date was before customer start date.").invalidNel
        } else customerRaw.validNel
      }

    implicit def CustomerRawValidator: DatasourceValidator[CustomerRaw] = DatasourceValidator.instance {
      rawCustomer: CustomerRaw =>
        (
          customerDatesRule(rawCustomer),
          //For simple validation rules, we can use helper methods as below.
          rawCustomer.gender.validateOption(_.stringMatches("M|F".r, "GenderRule", "gender")),
          rawCustomer.personal_bus_flg.validateOption(_.stringMatches("B|P".r, "BusinessFlagRule", "personal_bus_flg"))
        ).mapN { case _ => rawCustomer }
    }
  }

  object AccountRaw {

    implicit def AccountRawValidator: DatasourceValidator[AccountRaw] = DatasourceValidator.instance {
      rawAccount: AccountRaw =>
        rawAccount.acc_stat.validateOption(_.stringMatches("Active|Closed".r, "AccountStatusRule", "acc_stat")).map { case _ => rawAccount }
    }
  }

  object AccToCusRaw {

    implicit def AccountRawValidator: DatasourceValidator[AccToCusRaw] = DatasourceValidator.instance {
      accToCusRaw: AccToCusRaw =>
        accToCusRaw.data_source_ctry.validateOption(_.stringMatches("[A-Z]{2}".r, "CountryCodeRule", "data_source_ctry")).map { case _ => accToCusRaw }
    }
  }
}
