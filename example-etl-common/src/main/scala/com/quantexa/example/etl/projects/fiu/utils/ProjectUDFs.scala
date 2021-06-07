package com.quantexa.example.etl.projects.fiu.utils

import com.quantexa.etl.utils.cleansing.Business.isBusiness
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.valuesToNull
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf

/*
  * Project UDFs (user defined functions) generally used to apply functions to dataframe columns, shared across input data sources
  */

object ProjectUDFs {
  /**
   *  Converts a String field containing Y/N or 1/0 values into a nullable Boolean
   */
  val yesNoToBoolean: UserDefinedFunction = udf[Option[Boolean], String](s => if (s == null) None else Some(s.toLowerCase == "y" || s == "1"))

  /*
  * UDFs utilising the valuesToNull ETL utility function to convert blank strings and .'s to null
  */
  val blankToNullUDF: UserDefinedFunction = udf(valuesToNull(_:String, ""))
  val dotToNullUDF: UserDefinedFunction = udf(valuesToNull(_:String, "."))

  /* Use Quantexa's isBusiness function as UDF to categorise a name as a business or individual */
  val isBusinessUDF: UserDefinedFunction = udf { nameString: String => isBusiness(Option(nameString)) }
}
