/**
 * Copyright (c) 2012, Christer Sandberg
 */

package se.fishtank.css.selectors.parser

import Combinator._

object Selector {
  val UniversalTag: String = "*"

  def apply(tagName: String, specifiers: List[Specifier]): Selector =
    Selector(tagName, Descendant, specifiers)

  def apply(specifiers: List[Specifier]): Selector =
    Selector(UniversalTag, Descendant, specifiers)
}

case class Selector(tagName: String, combinator: Combinator, specifiers: List[Specifier]) {
  override def toString: String = (tagName, combinator, specifiers) match {
    case (Selector.UniversalTag, Descendant, Nil) => tagName
    case (Selector.UniversalTag, c, Nil) => "%s %s".format(c, tagName)
    case (Selector.UniversalTag, Descendant, s) => s.mkString
    case (Selector.UniversalTag, c, s) => "%s %s".format(c, s.mkString)
    case (t, Descendant, s) => t + s.mkString
    case _ => "%s %s%s".format(combinator, tagName, specifiers.mkString)
  }
}

case class SelectorGroup(selectors: List[Selector]) {
  override def toString: String = selectors.mkString(" ")
}
