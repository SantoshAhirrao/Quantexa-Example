package com.quantexa.example.taskloading.utils

import java.util.concurrent.CancellationException

import cats.effect._
import cats.syntax.all._

import scala.concurrent.duration._

object GeneralUtils {

  /**
    * Turns an integer into a string which when sorted gives the same ordering as when sorting by number
    * E.g. 123 -> 000123
    * This can be useful to sort task lists by score until a change to the Quantexa product is made
    * by placing this 'string score' in the assignee field for example
    */
  def createStringSortableScore(score: Int): String = {
    assert(score >= 0)
    String.format("%6s", score.toString).replace(" ", "0")
  }

  /**
    * Repeat an IO every specified duration, failing if operation hasn't satisfied breakCondition before maxRepeats
    */
  def repeatLimitedTimes[A](ioa: IO[A], breakCondition: (A => Boolean), delay: FiniteDuration, maxRepeats: Int)
                           (implicit timer: Timer[IO]): IO[A] = {
    if (maxRepeats > 0) {
      ioa.flatMap(a => {
        if (breakCondition.apply(a)) {
          IO(a)
        }
        else {
          IO.sleep(delay) *> repeatLimitedTimes(ioa, breakCondition, delay, maxRepeats - 1)
        }
      })
    }
    else IO.raiseError(new CancellationException(s"Max number of retries ($maxRepeats) reached"))
  }

}
