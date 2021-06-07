package com.quantexa.example.scoring.scores.fiu.testdata

import com.quantexa.example.model.fiu.scoring.EdgeAttributes.BusinessEdge

object EdgeTestData {
  
  val testBusinessEdge = BusinessEdge(
    total_records = None,
    businessOnHotlist = Some("true")
  )
  
}