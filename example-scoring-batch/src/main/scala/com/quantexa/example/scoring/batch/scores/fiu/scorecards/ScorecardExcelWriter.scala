package com.quantexa.example.scoring.batch.scores.fiu.scorecards

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.sql.Date
import java.time.format.DateTimeFormatter

import com.quantexa.example.scoring.batch.model.fiu.ScoringModels
import com.quantexa.example.scoring.batch.model.fiu.ScoringModels.LookupKey
import com.quantexa.example.scoring.batch.utils.fiu.excel.ExcelTableStyles.tableStyleMedium5
import com.quantexa.example.scoring.batch.utils.fiu.excel.{CreateExcelTableWorksheet, FieldMapping, SheetConfiguration}
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.spark.sql.Dataset
import org.slf4j.{Logger, LoggerFactory}



case class ScoredUnderlyingScores(scoreId: String,
                                   subject: String,
                                  scorecardScore: Double,
                                  keyValues: Array[LookupKey], //mkstring with name value from lookup, e.g. customerId: Cust001, date: 3 Mar 2018
                                  docType: String,
                                  severity: Option[Int] = None,
                                  band: Option[String] = None,
                                  description: Option[String] = None){

}

case class ScorecardOverallScore(subject: String, scorecardScore: Double)

case class ScoredCustomerDetail(subject: String,
                                scorecardScore: Double,
                                key: String,
                                candidateContribution: Double,
                                contribution: Option[Double],
                                group: Option[String],
                                severity: Option[Int],
                                description: Option[String])

case class ScorecardExcelWriter(config: ProjectExampleConfig) extends Serializable {

  def writeScorecardToExcel(overallScore: Dataset[ScorecardOverallScore],
                            scoreDetail: Dataset[ScoredCustomerDetail],
                            underlyingScores:Dataset[ScoredUnderlyingScores],
                             outputFileFullPath: String) = {

    val overallScoreConfiguration = SheetConfiguration("ScorecardOverallScore",
      (data: ScorecardOverallScore) => List(FieldMapping("Subject", data.subject),
        FieldMapping("Score", data.scorecardScore)),
      tableStyleMedium5)

    val scoredCustomerDetailFieldMapping = (data:ScoredCustomerDetail) => List(
      FieldMapping("Subject", data.subject),
      FieldMapping("Score", data.scorecardScore),
      FieldMapping("Score Id", data.key),
      FieldMapping("Candidate Contribution", data.candidateContribution),
      FieldMapping("Contribution", data.contribution.getOrElse(0)),
      FieldMapping("Group", data.group.getOrElse("")),
      FieldMapping("Severity", data.severity.getOrElse(0)),
      FieldMapping("Description", data.description.getOrElse("")))


    val underlyingScoresFieldMapping = (data:ScoredUnderlyingScores) => {
      val sortedLookups: List[ScoringModels.LookupKey] = data.keyValues.sortWith(_.name < _.name).toList
      List(
        FieldMapping("Subject", data.subject),
        FieldMapping("Transaction Id", sortedLookups.find(_.name == "transactionId").flatMap(_.stringKey).getOrElse("")),
        FieldMapping("Analysis Date", sortedLookups.find(_.name == "analysisDate").flatMap(_.dateKey.map(ScorecardExcelWriter.formatSqlDate)).getOrElse("")),
        FieldMapping("Related Score Id", data.scoreId),
        FieldMapping("Overall Score", data.scorecardScore),
        FieldMapping("Document Type", data.docType),
        FieldMapping("Severity", data.severity.getOrElse(0)),
        FieldMapping("Band", data.band.getOrElse("")),
        FieldMapping("Description", data.description.getOrElse("")))
    }
    val scoreDetailConfiguration = SheetConfiguration("CustomerLevelScores",
      scoredCustomerDetailFieldMapping,
      tableStyleMedium5)

    val underlyingScoresConfiguration = SheetConfiguration("UnderlyingScores",
      underlyingScoresFieldMapping,
      tableStyleMedium5)

    val wb = new XSSFWorkbook()
    val createScoreWorkbook = CreateExcelTableWorksheet(overallScore,overallScoreConfiguration) _ andThen CreateExcelTableWorksheet(scoreDetail, scoreDetailConfiguration) _ andThen CreateExcelTableWorksheet(underlyingScores, underlyingScoresConfiguration) _

    val scoringWorkbook = createScoreWorkbook(wb)


    Files.createDirectories(Paths.get(outputFileFullPath).getParent)
    val outputStream = new FileOutputStream(new File(outputFileFullPath))
    scoringWorkbook.write(outputStream)
  }
}

object ScorecardExcelWriter {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val scoreCardDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val maxLookups = 5
  def formatSqlDate(date: Date): String = scoreCardDateFormat.format(date.toLocalDate)
}
