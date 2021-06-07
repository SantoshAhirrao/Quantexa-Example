package com.quantexa.example.model.fiu.transaction

import cats.implicits._
import com.quantexa.etl.validation.row.{DatasourceValidator, ValidationError, ValidationResult}

object TransactionRawModel {

  case class TransactionRaw(
                             txn_id: Option[String] = None,
                             bene_acc_no: Option[Long] = None,
                             orig_acc_no: Option[Long] = None,
                             bene_name: Option[String] = None,
                             bene_ctry: Option[String] = None,
                             bene_addr: Option[String] = None,
                             orig_name: Option[String] = None,
                             orig_addr: Option[String] = None,
                             orig_ctry: Option[String] = None,
                             posting_dt: Option[java.sql.Timestamp] = None,
                             txn_amt: Option[Double] = None,
                             txn_source_typ_cde: Option[String] = None,
                             txn_desc: Option[String] = None,
                             cr_dr_cde: Option[String] = None,
                             bene_bank_ctry: Option[String] = None,
                             orig_bank_ctry: Option[String] = None,
                             txn_amt_base: Option[Double] = None,
                             run_dt: Option[java.sql.Timestamp] = None,
                             source_sys: Option[String] = None,
                             source_txn_no: Option[Long] = None,
                             txn_currency: Option[String] = None,
                             base_currency: Option[String] = None
                           )

  object TransactionRaw {

    //For custom logic, validation rules can either be written as below:
    val nonEmptyOptionRule: (Option[_], String) => ValidationResult[Option[_]] = (opt, fieldName) => {
      if(opt.isDefined) opt.validNel else {
        ValidationError("EmptyOptionRule", fieldName, s"$fieldName cannot be empty.").invalidNel
      }
    }

    implicit def TransactionRawValidator: DatasourceValidator[TransactionRaw] = DatasourceValidator.instance {
      transactionRaw: TransactionRaw =>
        (
          nonEmptyOptionRule(transactionRaw.txn_id, "txn_id"),
          nonEmptyOptionRule(transactionRaw.bene_acc_no, "bene_acc_no"),
          nonEmptyOptionRule(transactionRaw.orig_acc_no, "orig_acc_no"),
          nonEmptyOptionRule(transactionRaw.cr_dr_cde, "cr_dr_cde"),
          nonEmptyOptionRule(transactionRaw.posting_dt, "posting_dt"),
          nonEmptyOptionRule(transactionRaw.run_dt, "run_dt"),
          nonEmptyOptionRule(transactionRaw.txn_amt, "txn_amt"),
          nonEmptyOptionRule(transactionRaw.txn_currency, "txn_currency"),
          nonEmptyOptionRule(transactionRaw.txn_amt_base, "txn_amt_base"),
          nonEmptyOptionRule(transactionRaw.base_currency, "base_currency")
        ).mapN { case _ => transactionRaw }
    }
  }

}