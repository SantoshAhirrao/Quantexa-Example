package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.analytics.spark.sql.functions.{CategoryCountUDAF, CategoryCounter}
import com.quantexa.example.scoring.batch.scores.fiu.facts.CustomerDateStatsConfiguration
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StringType

//TODO: IP-201 Replace CombineMaps and CountCountryUse with CategoryCountUDAF
import org.apache.spark.sql.Encoders

import scala.collection.immutable.ListMap
object RollingCustomerPrevCountries {
  import com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate.CommonWindows.allTimeShift1Day
  private val stringCategoryCounterEncoder = Encoders.product[CategoryCounter[String]]
  private val customerLevelUdaf = CategoryCountUDAF.genericCategoryUDAF[String](None)(Encoders.STRING)
  private val rollingUdaf = CategoryCountUDAF.genericCategoryUDAF[CategoryCounter[String]](None)(stringCategoryCounterEncoder)
  private val coalescedCounterPartyAccountCountryColumn = coalesce(col("counterpartyAccountCountry"), lit("unknown")).cast(StringType)
  //This aids verification as the counts in the map will always total the number of transactions counted, whether counting beneficiary or originator (or both) will always be the same.
  private val notApplicableColumn = lit("N/A").cast(StringType)

  val configuration: CustomerDateStatsConfiguration = {
    CustomerDateStatsConfiguration(
      customerDateLevelStats = ListMap(
        "numberOfTransactions" -> count("*"),
        "totalValueTransactions" -> sum("analysisAmount"),
        "beneficiaryCountriesToday" -> customerLevelUdaf(when(col("customerOriginatorOrBeneficiary") === "originator", coalescedCounterPartyAccountCountryColumn).otherwise(notApplicableColumn)),
        "originatorCountriesToday" -> customerLevelUdaf(when(col("customerOriginatorOrBeneficiary") === "beneficiary", coalescedCounterPartyAccountCountryColumn).otherwise(notApplicableColumn))
      ),
      rollingStats = ListMap(
        "previousOriginatorCountries" -> rollingUdaf(col("originatorCountriesToday")).over(allTimeShift1Day),
        "previousBeneficiaryCountries" -> rollingUdaf(col("beneficiaryCountriesToday")).over(allTimeShift1Day))
      )
  }
}
