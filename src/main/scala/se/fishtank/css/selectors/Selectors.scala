/**
 * Copyright (c) 2012, Christer Sandberg
 */

package se.fishtank.css.selectors

import parser._
import parser.Combinator._
import parser.Selector._
import parser.Specifier._

import xml.{Elem, Node, Text}

object Selectors {
  def query(selectorString: String, root: Elem): Either[String, List[Node]] =
    new Selectors(root).query(selectorString)

  def query(selectorGroups: List[SelectorGroup], root: Elem): List[Node] =
    new Selectors(root).query(selectorGroups)
}

class Selectors(val root: Elem) {
  import Zipper._

  val rootLoc = Zipper(root)

  def query(selectorString: String): Either[String, List[Node]] = {
    SelectorParser.parse(selectorString) match {
      case SelectorParser.Failure(msg) => Left(msg)
      case SelectorParser.Success(selectorGroups) => Right(query(selectorGroups))
    }
  }

  def query(selectorGroups: List[SelectorGroup]): List[Node] = {
    // TODO: Make this tail recursive.
    def loop(xs: List[Selector]): List[Location] = xs.foldLeft(rootLoc :: Nil) { (ys, selector) =>
      val byTagName = queryByTagNameAndCombinator(selector, ys)
      selector.specifiers.foldLeft(byTagName) { (ys, specifier) =>
        specifier match {
          case a: Attribute => queryByAttribute(a, ys)
          case p: PseudoClass => queryByPseudoClass(p.value, ys)
          case p: PseudoElement => ys
          case p: PseudoNth => queryByPseudoNth(p, ys)
          case n: Negation => ys diff loop(n.selector :: Nil)
        }
      }
    }

    selectorGroups.foldLeft(List.empty[Node]) { (xs, group) =>
      loop(group.selectors).map(_.node)(collection.breakOut)
    }

    // Need to get rid of duplicates. Not a very nice solution, but it works.
    import collection.JavaConversions._
    val set = new java.util.LinkedHashSet[Node]()
    for (group <- selectorGroups) {
      val res = loop(group.selectors).map(_.node)
      set ++= res
    }
    
    set.toList
  }

  private def queryByTagNameAndCombinator(selector: Selector, xs: List[Location]): List[Location] = {
    val tagName = tagNameFilter(selector)
    selector.combinator match {
      case Descendant =>
        xs match {
          case head :: Nil if head == rootLoc =>
            xs.flatMap(_.descendantsOrSelf(_.node.isInstanceOf[Elem])).filter(tagName)
          case _ =>
            xs.flatMap(_.descendants(_.node.isInstanceOf[Elem])).filter(tagName)
        }

      case Child =>
        xs.flatMap(_.children(tagName))
      case AdjacentSibling =>
        xs.flatMap(_.findNext(tagName).toList)
      case GeneralSibling =>
        val seen = collection.mutable.HashSet[Int]()
        xs.flatMap(_.following(l => tagName(l) && seen.add(l.id)))
    }
  }
  
  private def queryByAttribute(attr: Attribute, xs: List[Location]): List[Location] = xs.filter { loc =>
    (loc.node.attribute(attr.name), attr.matches) match {
      case (None, _) => false
      case (_, None) => true // Attribute is present.
      case (Some(s), Some((m, v))) =>
        val txt = s.text.trim
        if (txt.isEmpty) false else m match {
          case Attribute.Exact => txt == v
          case Attribute.Hyphen => txt == v || txt.startsWith(v + '-')
          case Attribute.Prefix => txt.startsWith(v)
          case Attribute.Suffix => txt.endsWith(v)
          case Attribute.Contains => txt.contains(v)
          case Attribute.List => txt.split("\\s+").exists(_ == v)
        }
    }
  }
  
  private def queryByPseudoClass(value: PseudoClass.Value, xs: List[Location]): List[Location] = value match {
    case PseudoClass.Root => xs.filter(_.isTop)
    case PseudoClass.Empty =>
      xs.filterNot(loc => loc.node.child.exists {
        _ match {
          case Text(txt) => txt.nonEmpty
          case _ => true
        }
      })
    case PseudoClass.FirstChild =>
      xs.filter(prevElem(_).isEmpty)
    case PseudoClass.LastChild =>
      xs.filter(nextElem(_).isEmpty)
    case PseudoClass.OnlyChild =>
      xs.filter(l => prevElem(l).isEmpty && nextElem(l).isEmpty)
    case PseudoClass.FirstOfType =>
      xs.filter(l => prevElemByName(l, l.node.label).isEmpty)
    case PseudoClass.LastOfType =>
      xs.filter(l => nextElemByName(l, l.node.label).isEmpty)
    case PseudoClass.OnlyOfType =>
      xs.filter(l => prevElemByName(l, l.node.label).isEmpty && nextElemByName(l, l.node.label).isEmpty)
  }
  
  private def queryByPseudoNth(nth: PseudoNth, xs: List[Location]): List[Location] = nth.value match {
    case PseudoNth.NthChild =>
      xs.filter(l => nth.isMatch(l.preceding(isElem).length + 1))
    case PseudoNth.NthLastChild =>
      xs.filter(l => nth.isMatch(l.following(isElem).length + 1))
    case PseudoNth.NthOfType =>
      xs.filter(l => nth.isMatch(l.preceding(elemByName(_, l.node.label)).length + 1))
    case PseudoNth.NthLastOfType =>
      xs.filter(l => nth.isMatch(l.following(elemByName(_, l.node.label)).length + 1))
  }

  private def tagNameFilter(selector: Selector): Zipper.Filter = loc => loc.node match {
    case e: Elem => selector.tagName == UniversalTag || selector.tagName == e.label
    case _ => false
  }
  
  private def isElem(loc: Location): Boolean = loc.node.isInstanceOf[Elem]
  
  private def prevElem(loc: Location): Option[Location] = loc.findPrevious(isElem)
  
  private def nextElem(loc: Location): Option[Location] = loc.findNext(isElem)
  
  private def elemByName(loc: Location, name: String): Boolean = isElem(loc) && loc.node.label == name
  
  private def prevElemByName(loc: Location, name: String): Option[Location] = loc.findPrevious(elemByName(_, name))
  
  private def nextElemByName(loc: Location, name: String): Option[Location] = loc.findNext(elemByName(_, name))
}
