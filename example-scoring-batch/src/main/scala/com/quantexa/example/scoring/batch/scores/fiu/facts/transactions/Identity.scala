package com.quantexa.example.scoring.batch.scores.fiu.facts.transactions

import com.quantexa.example.scoring.batch.model.fiu.ScoringModels.CreateFacts
import org.apache.spark.sql.DataFrame

object Identity extends CreateFacts with Serializable{
  def addFacts(df:DataFrame) : DataFrame = {
    df
  }
}