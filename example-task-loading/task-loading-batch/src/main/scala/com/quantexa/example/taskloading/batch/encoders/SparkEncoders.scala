package com.quantexa.example.taskloading.batch.encoders

import cats.effect.IO
import com.quantexa.example.taskloading.model.Model.TaskNameToTaskRequest
import org.apache.spark.sql.Encoders

/**
  * All Spark Encoders should be placed in this object
  */
object SparkEncoders {

  implicit val stringIOStringEncoder = Encoders.tuple(Encoders.STRING, Encoders.kryo[IO[String]])
  implicit val stringEitherEncoder = Encoders.tuple(Encoders.STRING, Encoders.kryo[Either[Throwable, String]])
  implicit val taskNameToTaskRequestEncoder = Encoders.kryo[TaskNameToTaskRequest]

}
