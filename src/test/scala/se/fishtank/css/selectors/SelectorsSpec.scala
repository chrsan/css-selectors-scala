package se.fishtank.css.selectors

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class SelectorsSpec extends FlatSpec with ShouldMatchers with TestData {
  "Querying" should "return the expected number of nodes" in {
    for ((selector, expected) <- TestData) {
      Selectors.query(selector, Html) match {
        case Left(err) => fail(err)
        case Right(result) => result.length should equal (expected)
      }
    }
  }
}
