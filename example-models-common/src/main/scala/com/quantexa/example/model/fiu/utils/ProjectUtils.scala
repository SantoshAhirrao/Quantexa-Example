package com.quantexa.example.model.fiu.utils

//TODO: ScalaDoc
object ProjectUtils {
  def smartFormatter(amt: Option[Double], cnt: Option[Long]): String = {
    val amount = amt.getOrElse(0.0)
    val count = cnt.getOrElse(0L)
    val (amountToPrint, unit) = if (amount > 1e6)
      (amount / 1e6, " Million")
    else if (amount > 10e3)
      (amount / 10e3, " Thousand")
    else (amount, "")
    val suffix = if (count == 1L) " txn" else " txns"
    "$" + "%.1f".format(amountToPrint) + unit + " (" + count.toString + suffix + ")"
  }
}