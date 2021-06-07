package com.quantexa.example.model.fiu.hotlist

import com.quantexa.model.core.datasets.ParsedDatasets.{StandardParsedAttributes => attr, StandardParsedCompounds => comp, StandardParsedElements => elem, _}
import com.quantexa.resolver.ingest.Model._
import com.quantexa.resolver.ingest.extractors.DataModelExtractor
import com.quantexa.resolver.ingest.BrokenLenses
import com.quantexa.security.shapeless.Redact._
import shapeless.Typeable

object HotlistModel {

  /***
    * Case class to represent a Hotlist in the FIU data
    * A hotlist record can reference either an individual or a business
    * @param hotlistId               Primary Key for a Hotlist record
    * @param dateAdded               Date record added to hotlist
    * @param registeredBusinessName      Registered Business Name
    * @param parsedBusinessName          Parsed Business Name
    * @param personalBusinessFlag         Personal/Business
    * @param dateOfBirth              Individual Date of Birth
    * @param dateOfBirthParts                Parsed Date of Birth
    * @param fullName                Individual Full Name
    * @param parsedIndividualName          Parsed Individual Name
    * @param consolidatedAddress     Full Hotlist Address
    * @param parsedAddress           Parsed Hotlist Address
    * @param telephoneNumber                   Hotlist Telephone Number
    * @param cleansedTelephoneNumber          Cleansed Hotlist Telephone Number
    */
  case class Hotlist(
    @id hotlistId: Long, //ID is not optional
    dateAdded: Option[java.sql.Date],
    registeredBusinessName: Option[String],
    parsedBusinessName: Option[LensParsedBusiness[Hotlist]],
    personalBusinessFlag: Option[String],
    dateOfBirth: Option[java.sql.Date],
    dateOfBirthParts: Option[ParsedDateParts],
    fullName: Option[String],
    parsedIndividualName: Seq[LensParsedIndividualName[Hotlist]], //Seq is not optional
    consolidatedAddress: Option[String],
    parsedAddress: Option[LensParsedAddress[Hotlist]],
    telephoneNumber: Option[String],
    cleansedTelephoneNumber: Option[LensParsedTelephone[Hotlist]])

  object Hotlist {
    import com.quantexa.security.shapeless.Redact
    import com.quantexa.security.shapeless.Redact.Redactor
    val redactor: Redactor[Hotlist] = Redact.apply[Hotlist]
  }
}

object Elements {
  import BrokenLenses._
  import BOpticOps._
  import HotlistModel._

  //Label
  val label = traversal[Hotlist, String](i => Some(i.hotlistId.toString + " - " + i.fullName.getOrElse("") + i.registeredBusinessName.getOrElse("")))

  //Business
  val registeredBusinessName = elementLens[Hotlist]("registeredBusinessName")(_.registeredBusinessName)
  val parsedBusinessName = elem.businessElements[Hotlist]
  val dateAdded = elementLens[Hotlist]("dateAdded")(_.dateAdded.map(_.toString))
  val dateAddedLong = traversal[Hotlist,java.sql.Date](_.dateAdded)

  // Individual
  val fullName = elementLens[Hotlist]("fullName")(_.fullName)

  // Telephone
  val cleansedTelephoneNumber = elem.telephone[Hotlist]

  // Address
  val parsedAddress = elem.addressElements[Hotlist]

  // to be used for linking attributes defined below
  val onHotlist = lens[Hotlist, String](x => "true")
}

object Compounds {
  import Elements._
  import HotlistModel._
  import com.quantexa.resolver.ingest.BrokenLenses.BOpticOps._

  // Document Attribute Definition
  val document_labels = DocumentAttributeDefinitionList[Hotlist](
    NamedDocumentAttribute("label",label)
    , NamedDocumentAttribute("dateAdded",dateAddedLong)
  )

  // Traversals
  val hotlistToTelephone = traversal[Hotlist, LensParsedTelephone[Hotlist]](_.cleansedTelephoneNumber)
  val hotlistToDateOfBirth = traversal[Hotlist, ParsedDateParts](_.dateOfBirthParts)
  val hotlistToBusiness = traversal[Hotlist, LensParsedBusiness[Hotlist]](_.parsedBusinessName)
  val hotlistToAddress = traversal[Hotlist, LensParsedAddress[Hotlist]](_.parsedAddress)

  /* ---------------------------------------------------*/
  /*---------------------Individual--------------------*/
  /*--------------------------------------------------*/

  // Compounds
  val individualToHotlist = lens[LensParsedIndividualName[Hotlist], Hotlist](_.parent)

  val individualToDateOfBirth = individualToHotlist ~> hotlistToDateOfBirth
  val individualToBusiness = individualToHotlist ~> hotlistToBusiness
  val individualToTelephone = individualToHotlist ~> hotlistToTelephone
  val individualToAddress = individualToHotlist ~> hotlistToAddress

  //All Compounds/Attributes from Entity Root (LensParsedIndividualName) to (elementLens)
  def individualComps = comp.individualCompoundDefs(
    toAddr = Some(individualToAddress)
    , toDoBParts = Some(individualToDateOfBirth)
    , toTel = Some(individualToTelephone)
    , toBiz = Some(individualToBusiness)
  )

  // Attributes
  val `parsedIndividualName..Hotlist` = lens[LensParsedIndividualName[Hotlist], Hotlist](_.parent)
  val individualDisplay = EntityAttributeDefinitionList(
    "individual"
    , NamedEntityAttribute("fullName", `parsedIndividualName..Hotlist` ~> fullName)
    , NamedEntityAttribute("dateAddedLong", `parsedIndividualName..Hotlist` ~> dateAddedLong)
    , NamedEntityAttribute("individualOnHotlist", `parsedIndividualName..Hotlist` ~> onHotlist)
  )

  /* ---------------------------------------------------*/
  /*-----------------------Address---------------------*/
  /*--------------------------------------------------*/

  //Compounds
  def addressCustomerCompounds = comp.addressCompoundDefs[Hotlist]

  //Attributes
  def addressCustomerAttributes = attr.addressAttributeDefs[Hotlist]

  /* ---------------------------------------------------*/
  /*-----------------------Business--------------------*/
  /*--------------------------------------------------*/

  // Compounds
  val businessToHotlist = lens[LensParsedBusiness[Hotlist], Hotlist](_.parent)

  val businessToAddress = businessToHotlist ~> hotlistToAddress
  val businessToTelephone = businessToHotlist ~> hotlistToTelephone

  def businessCustCompounds: CompoundDefinitionList[LensParsedBusiness[Hotlist]] = comp.businessCompoundDefs(
    toAddress = Some(businessToAddress)
    , toTelephone = Some(businessToTelephone)
  )

  // Attributes
  val businessDisplay = EntityAttributeDefinitionList(
    "business"
    , NamedEntityAttribute("business_name_display", elem.businessNameDisplay[Hotlist])
    , NamedEntityAttribute("label", businessToHotlist ~> registeredBusinessName)
    , NamedEntityAttribute("businessOnHotlist", businessToHotlist ~> onHotlist)
  )

  /* ---------------------------------------------------*/
  /*-----------------------Telephone-------------------*/
  /*--------------------------------------------------*/

  // Compounds
  val telephoneCompounds = CompoundDefinitionList("telephone",
    NamedCompound("telephoneClean", cleansedTelephoneNumber)
  )
  val telephoneDisplays = EntityAttributeDefinitionList("telephone",
    NamedEntityAttribute("telephoneDisplay", elem.telephoneDisplay[Hotlist])
  )

  implicit val HotlistTypeable = Typeable.simpleTypeable(classOf[Hotlist])
}

import com.quantexa.example.model.fiu.hotlist.Compounds.HotlistTypeable
object HotlistExtractor extends DataModelExtractor[HotlistModel.Hotlist](Compounds)
