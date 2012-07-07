/**
 * Copyright (c) 2012, Christer Sandberg
 */

package se.fishtank.css.selectors.parser

import Combinator._
import Specifier._
import Specifier.PseudoClass._
import Specifier.PseudoNth._

import scala.util.parsing.combinator.{ImplicitConversions, PackratParsers, RegexParsers}

object SelectorParser {
  sealed trait Result
  case class Failure(message: String) extends Result
  case class Success(selectorGroups: List[SelectorGroup]) extends Result

  def parse(input: String): Result = new SelectorParser().parse(input)
}

private[selectors] class SelectorParser extends RegexParsers with ImplicitConversions with PackratParsers {
  override def skipWhitespace = false

  lazy val nonAscii = """[\x80-\xFF]""".r

  lazy val escapedNl = """\\(?:\n|\r\n|\r|\f)""".r

  lazy val stringChars = """[\t !#$%&\(-~]""".r

  lazy val nth = """[+\-]?\d*n(?:\s*[+\-]\s*\d+)?|[+\-]?\d+|odd|even""".r

  lazy val s = """\s+""".r

  lazy val escape: PackratParser[String] =
    "\\" ~! ("""\p{XDigit}{1,6}\s?|[ \-~]""".r | nonAscii) ^^ { case a ~ b => a + b }

  lazy val nmStart: PackratParser[String] =
    """[_a-zA-Z]""".r | nonAscii | escape

  lazy val nmChar: PackratParser[String] =
    """[_a-zA-Z0-9\-]""".r | nonAscii | escape

  lazy val string1: PackratParser[String] =
    "\"" ~> rep(stringChars | escapedNl | "'" | nonAscii | escape) <~ "\"" ^^ (_.mkString)

  lazy val string2: PackratParser[String] =
    "'" ~> rep(stringChars | escapedNl | "\"" | nonAscii | escape) <~ "'" ^^ (_.mkString)

  lazy val string: PackratParser[String] = (string1 | string2)

  lazy val ident: PackratParser[String] =
    ("-"?) ~! nmStart ~! (nmChar*) ^^ { case a ~ b ~ c => "%s%s%s".format(a.getOrElse(""), b, c.mkString) }

  lazy val combinator: PackratParser[Combinator] =
    """\s*[+>~]\s*|\s+""".r ^^ {
      _.trim match {
        case Descendant.repr      => Descendant
        case Child.repr           => Child
        case AdjacentSibling.repr => AdjacentSibling
        case GeneralSibling.repr  => GeneralSibling
      }
    }

  lazy val hash: PackratParser[Attribute] =
    "#" ~> (nmChar*) ^^ { case x => Attribute("id", Some(Attribute.Exact -> x.mkString)) }

  lazy val clazz: PackratParser[Attribute] =
    "." ~> ident ^^ { case x => Attribute("class", Some(Attribute.List -> x)) }

  lazy val attribMatch: PackratParser[Option[(Attribute.Match, Attribute.Value)]] =
    opt("""\s*[~|^$*]?=\s*""".r ~! (string | ident)) ^^ {
      case None => None
      case Some(m ~ value) =>
        m.trim match {
          case Attribute.Exact.repr    => Some(Attribute.Exact -> value)
          case Attribute.List.repr     => Some(Attribute.List -> value)
          case Attribute.Hyphen.repr   => Some(Attribute.Hyphen -> value)
          case Attribute.Prefix.repr   => Some(Attribute.Prefix -> value)
          case Attribute.Suffix.repr   => Some(Attribute.Suffix -> value)
          case Attribute.Contains.repr => Some(Attribute.Contains -> value)
        }
    }

  lazy val attrib: PackratParser[Attribute] =
    "[" ~> rep(s) ~> ident ~! attribMatch <~ rep(s) <~ "]" ^^ { case name ~ m => Attribute(name, m) }

  lazy val pseudoClassOrNth: PackratParser[PseudoSpecifier] =
    not("not") ~> ":" ~> ident ~ opt("(" ~> rep(s) ~> nth <~ rep(s) <~ ")") ^? ({
      case Empty.repr ~ None              => PseudoClass(Empty)
      case Root.repr ~ None               => PseudoClass(Root)
      case FirstChild.repr ~ None         => PseudoClass(FirstChild)
      case LastChild.repr ~ None          => PseudoClass(LastChild)
      case OnlyChild.repr ~ None          => PseudoClass(OnlyChild)
      case FirstOfType.repr ~ None        => PseudoClass(FirstOfType)
      case LastOfType.repr ~ None         => PseudoClass(LastOfType)
      case OnlyOfType.repr ~ None         => PseudoClass(OnlyOfType)
      case NthChild.repr ~ Some(arg)      => PseudoNth(NthChild, arg)
      case NthLastChild.repr ~ Some(arg)  => PseudoNth(NthLastChild, arg)
      case NthOfType.repr ~ Some(arg)     => PseudoNth(NthOfType, arg)
      case NthLastOfType.repr ~ Some(arg) => PseudoNth(NthLastOfType, arg)
    }, { case v ~ _ => v + " is not a valid pseudo class!" })

  lazy val pseudoElement: PackratParser[PseudoElement] =
    "::" ~> ident ^^ { PseudoElement(_) }

  lazy val pseudo: PackratParser[PseudoSpecifier] =
    pseudoElement | pseudoClassOrNth

  lazy val negation: PackratParser[Negation] =
    ":not(" ~> rep(s) ~> negationSimpleSelector <~ rep(s) <~ ")" ^^ { case sel => Negation(sel) }

  lazy val specifier: PackratParser[Specifier] =
    negation | hash | clazz | attrib | pseudo

  lazy val tagSelector: PackratParser[Selector] =
    ("*" | ident) ~! rep(specifier) ^^ { case tag ~ specifiers => Selector(tag, specifiers) }

  lazy val specifiersSelector: PackratParser[Selector] =
    rep1(specifier) ^^ { case specifiers => Selector(specifiers) }

  lazy val simpleSelector: PackratParser[Selector] =
    (specifiersSelector | tagSelector) // <~ rep(s) // Added \s* in combinator.

  lazy val selectorWithCombinator: PackratParser[Selector] =
    combinator ~! simpleSelector ^^ { case c ~ sel => sel.copy(combinator = c) }

  lazy val selector: PackratParser[SelectorGroup] =
    simpleSelector ~! rep(selectorWithCombinator) ^^ { case sel ~ xs => SelectorGroup(sel :: xs) }

  lazy val selectorGroup: PackratParser[List[SelectorGroup]] =
    rep("," ~> rep(s) ~> selector)

  lazy val selectors: PackratParser[List[SelectorGroup]] =
    selector ~! selectorGroup ^^ { case group ~ xs =>  group :: xs }

  lazy val negationSpecifier: PackratParser[Specifier] =
    hash | clazz | attrib | pseudo

  lazy val negationTagSelector: PackratParser[Selector] =
    ("*" | ident) ~! rep(negationSpecifier) ^^ { case tag ~ specifiers => Selector(tag, specifiers) }

  lazy val negationSpecifiersSelector: PackratParser[Selector] =
    rep1(negationSpecifier) ^^ { case specifiers => Selector(specifiers) }

  lazy val negationSimpleSelector: PackratParser[Selector] =
    (negationSpecifiersSelector | negationTagSelector) <~ rep(s)

  def parse(input: String): SelectorParser.Result = {
    parseAll(selectors, input) match {
      case Success(result, _) => SelectorParser.Success(result)
      case x: Failure => SelectorParser.Failure(x.msg)
      case x: Error => SelectorParser.Failure(x.msg)
    }
  }
}
