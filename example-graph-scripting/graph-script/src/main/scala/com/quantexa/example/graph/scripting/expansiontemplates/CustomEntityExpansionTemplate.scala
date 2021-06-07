package com.quantexa.example.graph.scripting.expansiontemplates

import com.quantexa.graph.script.expansiontemplates.StandardDocumentExpansionTemplate
import com.quantexa.graph.script.expansiontemplates.SelectorImplicits._
import com.quantexa.resolver.core.EntityGraph.{Edge, Entity}
import com.quantexa.resolver.core.ResolverAPI._

/* All project-specific entity expansions should be defined inside this object. Some simple expansion templates
* have been defined in quantexa-graph-scripting-incubator under StandardEntityExpansionTemplate. If your project requires the
* standard expansions, then use them, but any additional expansion templates required should go here. */
object CustomEntityExpansionTemplate {

  val customExpansion = new ExplicitEntityExpansion(
    Seq(new EntityExpansionRequestSelector(
      SelectAll[Entity],
      SelectAll[Edge]
    )),
    Some(StandardDocumentExpansionTemplate.defaultDocumentExpansion)
  )
}
