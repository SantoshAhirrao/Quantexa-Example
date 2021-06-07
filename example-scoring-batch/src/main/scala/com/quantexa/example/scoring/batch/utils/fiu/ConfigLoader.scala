package com.quantexa.example.scoring.batch.utils.fiu

import com.quantexa.scriptrunner.util.CommandLine
import com.quantexa.scriptrunner.util.config.TypesafeConfigLoader
import com.quantexa.scriptrunner.util.encryption.TypesafeConfigDecryptor
import io.circe._
import io.circe.config.syntax.CirceConfigOps

object ConfigLoader {
  def extractCaseClass[A: Decoder](configPaths: Seq[String], configRoot: String): Either[Error, A] = {
    val cmdLineInput = CommandLine.empty.copy(configFiles = Some(configPaths.mkString(",")), configRoot = Some(configRoot))
    val typesafeConfig = TypesafeConfigLoader.load(Some(cmdLineInput))
    val decryptedConfig = TypesafeConfigDecryptor(typesafeConfig)
    decryptedConfig.as[A]
  }
}