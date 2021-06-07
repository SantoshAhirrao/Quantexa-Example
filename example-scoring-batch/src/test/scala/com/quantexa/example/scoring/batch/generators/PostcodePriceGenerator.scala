package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.model.fiu.lookupdatamodels.PostcodePrice
import org.scalacheck.Gen

import scala.reflect.ClassTag

case object PostcodePriceGenerator extends GeneratesDataset[PostcodePrice] {
// data sourced via gsutil cp gs://quantexa-test-data/fiu/raw/smoke/full/postcodePrices.csv ./
  val postCodePrices= Seq(
    PostcodePrice("SK58PA", 79553.44068d),
    PostcodePrice("TS37AL", 90704.41421d),
    PostcodePrice("TS68DE", 82111.31824d),
    PostcodePrice("TS67QY", 68005.7295d),
    PostcodePrice("CF53UH", 79710.97115d),
    PostcodePrice("WF105SD", 86686.7865d),
    PostcodePrice("WS117WT", 72185.22686d),
    PostcodePrice("NE615NY", 40321.99453d),
    PostcodePrice("SA732DL", 46602.54003d),
    PostcodePrice("DH79AJ", 68296.69524d),
    PostcodePrice("DL178AT", 67875.37076d),
    PostcodePrice("DN163DX", 67068.65231d),
    PostcodePrice("LL143DH", 77030.55328d),
    PostcodePrice("M314LP", 77970.0528d),
    PostcodePrice("S123DE", 91292.87031d),
    PostcodePrice("WF149DT", 96773.06046d),
    PostcodePrice("SA12NJ", 75056.35386d),
    PostcodePrice("S756GG", 95609.59848d),
    PostcodePrice("OL146SQ", 93502.05166d))

  def generate(implicit ct: ClassTag[PostcodePrice]): Gen[PostcodePrice] =
    Gen.oneOf(postCodePrices)

}
