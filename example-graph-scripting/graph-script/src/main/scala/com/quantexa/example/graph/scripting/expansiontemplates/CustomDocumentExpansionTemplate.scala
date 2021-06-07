package com.quantexa.example.graph.scripting.expansiontemplates

import com.quantexa.resolver.core.ResolverAPI._
import com.quantexa.resolver.core.configuration.Definitions.ResolutionTemplateName
import com.quantexa.graph.script.expansiontemplates.SelectorImplicits._
import com.quantexa.graph.script.expansiontemplates.{StandardDocumentExpansionTemplate, StandardEntityExpansionTemplate}
import com.quantexa.resolver.core.EntityGraph.{Document, Record, RecordWithAttributes}

/* All project-specific document expansions should be defined inside this object. Some simple expansion templates
* have been defined in quantexa-graph-scripting-incubator under StandardDocumentExpansionTemplate. If your project requires the
* standard expansions, then use them, but any additional expansion templates required should go here. */
object CustomDocumentExpansionTemplate {

  val defaultResolutionTemplateName = new ResolutionTemplateName("default")

  val customExpansion = new ExplicitDocumentExpansion(
    Seq(new DocumentExpansionRequestSelector(
      SelectAll[Document],
      SelectAll[Record],
      SelectAll[RecordWithAttributes],
      Map(
        "individual" -> defaultResolutionTemplateName,
        "business" -> defaultResolutionTemplateName
      )
    )),
    None
  )

  /* Only passing in Individual to the template Map limits expansions to Individual only.
   * An alternative way of doing this would be to pass an entity expansion with a selector of
   * EqualsStringSelector[Entity](EntityEntityTypeVarOp(), StringVarOp[Entity]("individual")) */
  val twoDegreeIndividualsOnly = new ExplicitDocumentExpansion(
    Seq(new DocumentExpansionRequestSelector(
      SelectAll[Document],
      SelectAll[Record],
      SelectAll[RecordWithAttributes],
      Map("individual" -> defaultResolutionTemplateName)
    )),
    Some(StandardEntityExpansionTemplate.defaultEntity)
  )

  val oneDegreeIndividualsOnly = new ExplicitDocumentExpansion(
    Seq(new DocumentExpansionRequestSelector(
      SelectAll[Document],
      RecordSelector.entityType === "individual",
      SelectAll[RecordWithAttributes],
      StandardDocumentExpansionTemplate.defaultTemplates
    )),
    None
  )

  /* Only passing in address to the template Map limits expansions to Address only */
  val twoDegreeAddressOnly = new ExplicitDocumentExpansion(
    Seq(new DocumentExpansionRequestSelector(
      SelectAll[Document],
      SelectAll[Record],
      SelectAll[RecordWithAttributes],
      Map("address" -> defaultResolutionTemplateName)
    )),
    Some(StandardEntityExpansionTemplate.defaultEntity)
  )

  val documentsOnly = new ExplicitDocumentExpansion(
    Seq(new DocumentExpansionRequestSelector(
      SelectAll[Document],
      SelectNone[Record],
      SelectNone[RecordWithAttributes],
      Map.empty
    )),
    None
  )

}
