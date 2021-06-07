package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.etl.core.elastic.Elastic6Deleter
import com.quantexa.etl.elastic.ElasticDeleteScript

object DeleteFromElastic extends ElasticDeleteScript(deleter = new Elastic6Deleter())
