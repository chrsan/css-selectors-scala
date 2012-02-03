/**
 * Copyright (c) 2012, Christer Sandberg
 */

package se.fishtank.css.selectors.parser

sealed abstract class Combinator(val repr: String) {
  override def toString: String = repr
}

object Combinator {
  case object Descendant extends Combinator("")
  case object Child extends Combinator(">")
  case object AdjacentSibling extends Combinator("+")
  case object GeneralSibling extends Combinator("~")
}
