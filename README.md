## Overview

This project is a [Scala][scala] implementation of the
W3C Selectors Level 3 specification. I've already created one such
implementation in Java, but [Scala][scala] is my language of choice on
the JVM nowadays.

## Motivation

There's some built in support for XPath like expressions in the
[Scala][scala] library, but they are quite rudimentary. I also wanted
to dig deeper into the parser combinators and this project seemed like
a good way to do that.

## Implementation details

The default implementation works with the XML classes provided by the
standard [Scala][scala] library, but the parser and its support
classes may be used independent of the actual XML implementation.

Pseudo elements are parsed but not used when selecting nodes. It
doesn't make much sense to look for `a::hover` etc outside of a web browser.

See example usage below for more info.

## Supported selectors

* `*` any element
* `E` an element of type E
* `E[foo]` an E element with a "foo" attribute
* `E[foo="bar"]` an E element whose "foo" attribute value is exactly equal to "bar"
* `E[foo~="bar"]` an E element whose "foo" attribute value is a list of whitespace-separated values, one of which is exactly equal to "bar"
* `E[foo^="bar"]` an E element whose "foo" attribute value begins exactly with the string "bar"
* `E[foo$="bar"]` an E element whose "foo" attribute value ends exactly with the string "bar"
* `E[foo*="bar"]` an E element whose "foo" attribute value contains the substring "bar"
* `E[foo|="en"]` an E element whose "foo" attribute has a hyphen-separated list of values beginning (from the left) with "en"
* `E:root` an E element, root of the document or the root element specified
* `E:nth-child(n)` an E element, the n-th child of its parent
* `E:nth-last-child(n)` an E element, the n-th child of its parent, counting from the last one
* `E:nth-of-type(n)` an E element, the n-th sibling of its type
* `E:nth-last-of-type(n)` an E element, the n-th sibling of its type, counting from the last one
* `E:first-child` an E element, first child of its parent
* `E:last-child` an E element, last child of its parent
* `E:first-of-type` an E element, first sibling of its type
* `E:last-of-type` an E element, last sibling of its type
* `E:only-child` an E element, only child of its parent
* `E:only-of-type` an E element, only sibling of its type
* `E:empty` an E element that has no children (including text nodes)
* `E#myid` an E element with ID equal to "myid".
* `E:not(s)` an E element that does not match simple selector s
* `E F` an F element descendant of an E element
* `E > F` an F element child of an E element
* `E + F` an F element immediately preceded by an E element
* `E ~ F` an F element preceded by an E element

## Example usage (for the default implementation)

Suppose you have a `scala.xml.Elem` in a variable named `elem` that
you'd like to query using CSS selectors:

```scala
val result = Selectors.query("div:nth-child(2n)", elem)
```

This will return an `Either[String, List[Node]]` where `Left`
indicates a parser error. I.e. if the specified selector string
couldn't be parsed correctly.

It's also possible to query using a pre-parsed selector string. This
is the best choice if querying more than once for the same selector
string since it only needs to be parsed once.

```scala
val selectorGroups = SelectorParser.parse("div:nth-child(2n)") match {
  case SelectorParser.Success(selectorGroups) => selectorGroups
  case SelectorParser.Failure(msg) => error("Parse error: " + msg)
}

val nodes = Selectors.query(selectorGroups, elem)
```

## Build instructions

This project uses [SBT](http://www.scala-sbt.org/) as its build tool.

Releases are synced to Maven central via Sonatype.

```scala
"se.fishtank" % "css-selectors-scala" % version
```

The versions available can be found in the repo or by looking at the
tags for this project.

[scala]:http://scala-lang.org/
