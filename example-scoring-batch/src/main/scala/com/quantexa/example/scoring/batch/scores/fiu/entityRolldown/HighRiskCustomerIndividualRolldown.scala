package com.quantexa.example.scoring.batch.scores.fiu.entityrolldown

import com.quantexa.scoring.framework.spark.model.scores.CustomScoreWithConsumableOutput
import org.apache.spark.sql.SparkSession
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.scoring.batch.utils.fiu.Utils
import com.quantexa.example.scoring.batch.utils.fiu.Utils.CustomerRolldown
import com.quantexa.example.scoring.scores.fiu.entity._
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.ScoreTypes
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import org.apache.spark.sql.Dataset

//FIXME
class HighRiskCustomerIndividualRolldown(config: ProjectExampleConfig) extends CustomScoreWithConsumableOutput[CustomerRolldown, BasicScoreOutput] {
  val id = "HighRiskCustomerIndividualRolldown"

  import com.quantexa.scoring.framework.model.ScoreDefinition.implicits._

  val scoreToAggregate : ScoreTypes = Seq(HighRiskCustomerIndividual)

  def score(spark: SparkSession)(implicit scoreInput: ScoreInput): Dataset[(CustomerRolldown, Option[BasicScoreOutput])] = {
    import spark.implicits._

    val networkTable = Utils.getNetworkTable(s"${config.hdfsFolderCustomer}/eng/NetworkBuild/network_output.parquet")

    Utils.aggregateSingleEntityScoreType(config, (scoreToAggregate.head, "sum"), networkTable).as[(CustomerRolldown, Option[BasicScoreOutput])]
  }
}