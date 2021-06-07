package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.model.fiu.customer.CustomerModel.Account
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccToCusRaw
import org.scalacheck.Gen

import scala.reflect.ClassTag

case class AccToCusRawGenerator(accounts: Seq[Account]) extends GeneratesDataset[AccToCusRaw] {

  def generate(implicit ct: ClassTag[AccToCusRaw]): Gen[AccToCusRaw] = for {
    account <- Gen.oneOf(accounts)
  } yield AccToCusRaw(
    cus_id_no = Some(account.primaryAccountNumber),
    prim_acc_no = Some(account.primaryAccountNumber),
    acc_opened_dt = account.accountOpenedDate.map(x => new java.sql.Timestamp(x.getTime)),
    acc_closed_dt = account.accountClosedDate.map(x => new java.sql.Timestamp(x.getTime)),
    acc_link_created_dt = account.accountLinkCreatedDate.map(x => new java.sql.Timestamp(x.getTime)),
    acc_link_end_dt = account.accountLinkEndDate.map(x => new java.sql.Timestamp(x.getTime)),
    data_source_cde = account.dataSourceCode,
    data_source_ctry = account.dataSourceCountry,
    created_dt = None)
}