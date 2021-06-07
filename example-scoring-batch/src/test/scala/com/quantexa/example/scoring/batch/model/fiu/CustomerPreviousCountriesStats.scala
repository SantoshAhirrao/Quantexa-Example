package com.quantexa.example.scoring.batch.model.fiu

import com.quantexa.analytics.spark.sql.functions.CategoryCounter
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey

case class CustomerPreviousCountriesStats(
                                           key: CustomerDateKey,
                                           customerName: String,
                                           originatorCountriesToday: CategoryCounter[String],
                                           beneficiaryCountriesToday: CategoryCounter[String],
                                           previousOriginatorCountries: CategoryCounter[String],
                                           previousBeneficiaryCountries: CategoryCounter[String],
                                           numberOfTransactions: Long,
                                           totalValueTransactions: Double)
