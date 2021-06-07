package com.quantexa.example.scoring.batch.scores.fiu.integration

import com.quantexa.example.scoring.batch.utils.elastic.{GenericElasticLoadScript, GenericEs6Loader}
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerDateScoreOutput, CustomerRollup}

object LoadRollupTxnCustomerDateScoresToCustomerLevelElastic extends GenericElasticLoadScript[CustomerRollup[CustomerDateScoreOutput]](documentType = "txncustomerdatescoretocustomerrollup", documentIdField = "subject", loader = new GenericEs6Loader())