package com.quantexa.example.scoring.batch.scores.fiu.integration

import com.quantexa.example.scoring.batch.utils.elastic.{GenericElasticLoadScript, GenericEs6Loader}
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerRollup, TransactionScoreOutput}

object LoadRollupTxnScoreToCustomerLevelToElastic extends GenericElasticLoadScript[CustomerRollup[TransactionScoreOutput]](documentType = "txnscoretocustomerrollup", documentIdField = "subject", loader = new GenericEs6Loader())
