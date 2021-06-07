package com.quantexa.example.etl.projects.fiu

case class ETLConfig(
                      customer: DocumentConfig[CustomerInputFiles],
                      hotlist: DocumentConfig[HotlistInputFile],
                      transaction: DocumentConfig[TransactionInputFile],
                      scoringLookups: Option[ScoringLookups])

case class DocumentConfig[A](
                              documentType: String,
                              runId: Option[String],
                              isRunIdSetInConfig: Option[Boolean],
                              inputFiles: A,
                              datasourceRoot: String,
                              parquetRoot: String,
                              validatedParquetPath: String,
                              caseClassPath: String,
                              cleansedCaseClassPath: String,
                              metadataPath: String,
                              aggregatedCaseClassPath: Option[String] = None
                            )

case class ScoringLookups(
                           postcodePrices: String,
                           highRiskCountryCodes: String,
                           countryCorruption: String,
                           outputRoot: String)

case class CustomerInputFiles(
                               customer: String,
                               account: String,
                               acc_to_cus: String)

case class HotlistInputFile(hotlist: String)

case class TransactionInputFile(transaction: String, acc_to_cus: String)