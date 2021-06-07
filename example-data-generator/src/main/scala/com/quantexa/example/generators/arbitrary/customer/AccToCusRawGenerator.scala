package com.quantexa.example.generators.arbitrary.customer

import java.sql.Date.valueOf
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccToCusRaw
import com.quantexa.generators.model.DateRange
import com.quantexa.generators.templates.Generator
import com.quantexa.example.generators.config.GeneratorLoaderOptions
import org.scalacheck.{Arbitrary, Gen}
import com.quantexa.generators.utils.GeneratorUtils._

class AccToCusRawGenerator(generatorConfig: GeneratorLoaderOptions)  extends Generator[AccToCusRaw]{

  val accountStartDateRange = DateRange(valueOf("1990-01-06"), Some(valueOf("2009-12-26")))

  def getAccToCusRaw: Seq[AccToCusRaw] = {
    val arbitraryAccToCus = (id: Long) => Arbitrary {
      val seed = new java.util.Random(generatorConfig.seed*id).nextLong
      for {
        cus_id_no <- randomLongInRange(1L, generatorConfig.numberOfCustomerDocuments, seed).option(1)
        prim_acc_no <- id.option(1)
        accountStartRange = genDateRangeBetween(accountStartDateRange, seed).sample.get
        closedDate = new java.sql.Timestamp(accountStartRange.to.get.getTime).option(0.38, seed)
        acc_opened_dt <- new java.sql.Timestamp(accountStartRange.from.getTime).option(1)
        acc_closed_dt <- closedDate
        acc_link_created_dt <- new java.sql.Timestamp(accountStartRange.from.getTime).option(1)
        acc_link_end_dt <- closedDate
        data_source_cde <- "CUST ACCT LINK".option(1)
        data_source_ctry <- genCategory(generatorConfig.countryCodes, seed).option(1)
        created_dt <- new java.sql.Timestamp(accountStartRange.from.getTime).option(1)
      } yield AccToCusRaw(
        cus_id_no,
        prim_acc_no,
        acc_opened_dt,
        acc_closed_dt,
        acc_link_created_dt,
        acc_link_end_dt,
        data_source_cde,
        data_source_ctry,
        created_dt
      )
    }
    randomWithIds[AccToCusRaw](arbitraryAccToCus, generatorConfig.numberOfAccountDocuments, generatorConfig.seed)
  }

  override def generateRecords : Seq[AccToCusRaw] = {
    getAccToCusRaw
  }
}
