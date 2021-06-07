package com.quantexa.example.graph.scripting

import scala.concurrent.duration.FiniteDuration

case class StageConfig(
                       inputPath: String,
                       outputPath: String,
                       checkpoint: Boolean,
                       datasetRetries: Option[Int] = None
                      )

case class StageConfigTask(
                            inputPath: String,
                            outputPath: String,
                            checkpoint: Boolean,
                            taskListId: Option[String] = None
                          )

case class GraphScriptTimeoutsConfig(
                                      elasticTimeout: Option[FiniteDuration] = None,
                                      internalResolverTimeout: Option[FiniteDuration] = None,
                                      stageTimeout: Option[FiniteDuration] = None,
                                      resolverResponseTimeout: Option[FiniteDuration] = None,
                                      scoringResponseTimeout: Option[FiniteDuration] = None,
                                      documentResponseTimeout: Option[FiniteDuration] = None,
                                      taskResponseTimeout: Option[FiniteDuration] = None,
                                      investigationResponseTimeout: Option[FiniteDuration] = None,
                                      searchResponseTimeout: Option[FiniteDuration] = None
                                    )

case class GraphScriptingConfig(
                                 timeouts: GraphScriptTimeoutsConfig,
                                 resolverConfigPath: String,
                                 username: String,
                                 password: String,
                                 gatewayUri: String,
                                 oneHopStage: Option[StageConfig] = None,
                                 twoHopIndividualsOnlyStage: Option[StageConfig] = None,
                                 filterHighScoresStage: Option[StageConfig] = None,
                                 linkedRiskyAddressesStage: Option[StageConfig] = None,
                                 loadInvestigationStage: Option[StageConfig] = None,
                                 scoreCustomerOneHopNetworkStage: Option[StageConfig] = None,
                                 loadTaskStage: Option[StageConfigTask] = None
                               )