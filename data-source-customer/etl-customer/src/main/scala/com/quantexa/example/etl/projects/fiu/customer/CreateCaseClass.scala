package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.example.etl.projects.fiu.{CustomerInputFiles, DocumentConfig}
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.{timestampToDate, valuesToNull}
import com.quantexa.example.model.fiu.customer.CustomerModel.{Account, Customer}
import com.quantexa.example.model.fiu.customer.CustomerRawModel.{AccToCusRaw, AccountRaw, CustomerRaw}
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

/***
  * QuantexaSparkScript used to transform the raw validated Parquet files and create the hierarchical model for the FIU Smoke Test Data
  * Input: validated/parquet/customer.parquet, validated/parquet/acc_to_cus.parquet, validated/parquet/account.parquet
  * Output: DocumentDataModel/DocumentDataModel.parquet
  */
object CreateCaseClass extends TypedSparkScriptIncremental[DocumentConfig[CustomerInputFiles]] {
  val name = "CreateCaseClass - Customer"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: DocumentConfig[CustomerInputFiles], etlMetricsRepository: ETLMetricsRepository, metadataRunId: MetadataRunId): Unit = {
    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }

    import spark.implicits._

    val validatedParquetPath = projectConfig.validatedParquetPath
    val customerPath = validatedParquetPath + "/customer.parquet"
    val accountPath = validatedParquetPath + "/account.parquet"
    val accToCusPath = validatedParquetPath + "/acc_to_cus.parquet"
    val destinationPath = projectConfig.caseClassPath

    val accountRawDS = spark.read.parquet(accountPath).as[AccountRaw]
    val accToCusRawDS = spark.read.parquet(accToCusPath).as[AccToCusRaw]
    val customerRawDS = spark.read.parquet(customerPath).as[CustomerRaw]

    etlMetricsRepository.size("accountsDS", accountRawDS.count)
    etlMetricsRepository.size("accToCusDS", accToCusRawDS.count)
    etlMetricsRepository.size("customerDS", customerRawDS.count)

    val accountJoinDS = accToCusRawDS
      //TODO: This should be handled properly with validation
      .filter(_.prim_acc_no.isDefined)
      .join(accountRawDS.drop($"acc_opened_dt").drop($"acc_closed_dt"),
        Seq("prim_acc_no"), "left_outer").as[AccountIntermediateJoinStep]

    val accountDS = accountJoinDS.map(rawAccount =>
    Account(
      accountCustomerIdNumber = rawAccount.cus_id_no,
      primaryAccountNumber = rawAccount.prim_acc_no.get,
      accountOpenedDate = rawAccount.acc_opened_dt.map(timestampToDate),
      accountClosedDate = rawAccount.acc_closed_dt.map(timestampToDate),
      accountLinkCreatedDate = rawAccount.acc_link_created_dt.map(timestampToDate),
      accountLinkEndDate = rawAccount.acc_link_end_dt.map(timestampToDate),
      dataSourceCode = rawAccount.data_source_cde,
      dataSourceCountry = rawAccount.data_source_ctry,
      compositeAccountName = rawAccount.composite_acc_name,
      consolidatedAccountAddress = rawAccount.consolidated_acc_address,
      parsedAddress = None,
      accountStatus = rawAccount.acc_stat,
      accountDistrict = rawAccount.acc_district,
      relationalManagerName = valuesToNull(rawAccount.rel_mngr_name.getOrElse(""), ""),
      rawAccountName = rawAccount.raw_acc_name,
      addressLineOne = rawAccount.addr_line_one,
      city = rawAccount.city,
      state = valuesToNull(rawAccount.ctry.getOrElse(""), ""),
      postcode = rawAccount.postal_cde,
      country = rawAccount.ctry,
      accountRiskRating = rawAccount.acc_risk_rtng.map(_.toDouble),
      amlALRFlag = rawAccount.aml_alr_flg,
      telephoneNumber = valuesToNull(rawAccount.tel_no.getOrElse(""), ""),
      cleansedTelephoneNumber = None
    ))

    val customerDS = customerRawDS.filter(_.cus_id_no.isDefined)
      .map(rawCustomer =>
      Customer(
        customerIdNumber = rawCustomer.cus_id_no.get,
        customerIdNumberString = rawCustomer.cus_id_no.get.toString,
        customerStatus = rawCustomer.cus_stat,
        registeredBusinessName = valuesToNull(rawCustomer.bus_registered_name.getOrElse(""), ""),
        parsedBusinessName = None,
        registeredBusinessNumber = valuesToNull(rawCustomer.bus_registration_no.getOrElse("."), ".").map(_.toLong),
        countryOfRegisteredBusiness = valuesToNull(rawCustomer.bus_ctry_registration.getOrElse(""), ""),
        customerStartDate = rawCustomer.cus_start_dt.map(timestampToDate),
        customerEndDate = rawCustomer.cus_end_dt.map(timestampToDate),
        personalBusinessFlag = rawCustomer.personal_bus_flg,
        gender = valuesToNull(rawCustomer.gender.getOrElse(""), ""),
        dateOfBirth = rawCustomer.dt_of_birth.map(timestampToDate),
        dateOfBirthParts = None,
        initials = valuesToNull(rawCustomer.initials.getOrElse(""), ""),
        forename = valuesToNull(rawCustomer.forename.getOrElse(""), ""),
        familyName = valuesToNull(rawCustomer.family_name.getOrElse(""), ""),
        maidenName = valuesToNull(rawCustomer.maiden_name.getOrElse(""), ""),
        fullName = valuesToNull(rawCustomer.full_name.getOrElse(""), ""),
        parsedCustomerName = Seq(),
        consolidatedCustomerAddress = rawCustomer.consolidated_cust_address,
        parsedAddress = None,
        nationality = valuesToNull(rawCustomer.nationality.getOrElse(""), ""),
        personalNationalIdNumber = valuesToNull(rawCustomer.personal_national_id_no.getOrElse("."), ".").map(_.toLong),
        customerRiskRating = valuesToNull(rawCustomer.cus_risk_rtng.getOrElse("."), ".").map(_.toInt),
        employeeFlag = rawCustomer.employee_flag.map(f => if(f == "t") true else false),
        residenceCountry = rawCustomer.residence_ctry,
        accounts = Seq(),
        metaData = Some(metadataRunId)
      ))

    val accountsGroupedDS = accountDS.groupByKey { account => account.accountCustomerIdNumber }.mapGroups {
      case (acc_cus_id_no, accounts) =>
        (acc_cus_id_no, accounts.toSeq)
    }

    val customersWithAccounts = customerDS
      .joinWith(accountsGroupedDS, customerDS("customerIdNumber") === accountsGroupedDS("_1"), "left_outer")
      .map{
        case (customer,null) => customer
        case (customer,accountSeq) => customer.copy(accounts = accountSeq._2)
      }

    // Write metrics
    etlMetricsRepository.size("CreateCaseClass", customersWithAccounts.count)
    etlMetricsRepository.time("CreateCaseClass", customersWithAccounts.write.mode("overwrite").parquet(destinationPath))
  }
}

case class AccountIntermediateJoinStep(prim_acc_no: Option[Long],
                                        composite_acc_name: Option[String],
                                        consolidated_acc_address: Option[String],
                                        acc_stat: Option[String],
                                        acc_district: Option[String],
                                        rel_mngr_name: Option[String],
                                        raw_acc_name: Option[String],
                                        addr_line_one: Option[String],
                                        city: Option[String],
                                        state: Option[String],
                                        postal_cde: Option[String],
                                        ctry: Option[String],
                                        acc_risk_rtng: Option[Int],
                                        aml_alr_flg: Option[String],
                                        sar_count: Option[String],
                                        tel_no: Option[String],
                                        cus_id_no: Option[Long],
                                        acc_opened_dt: Option[java.sql.Timestamp],
                                        acc_closed_dt: Option[java.sql.Timestamp],
                                        acc_link_created_dt: Option[java.sql.Timestamp],
                                        acc_link_end_dt: Option[java.sql.Timestamp],
                                        data_source_cde: Option[String],
                                        data_source_ctry: Option[String],
                                        created_dt: Option[java.sql.Timestamp])
