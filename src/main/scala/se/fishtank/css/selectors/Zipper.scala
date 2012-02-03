/**
 * Copyright (c) 2012, Christer Sandberg
 */

package se.fishtank.css.selectors

import xml._

// Heavily influenced by this great blog entry: http://szeiger.de/blog/2009/12/27/a-zipper-for-scala-xml/

object Zipper {
  type Filter = Location => Boolean

  val NoFilter: Filter = _ => true

  def unfold(seed: Option[Location], filter: Filter)(f: Location => Option[Location]): List[Location] = {
    val b = List.newBuilder[Location]

    @annotation.tailrec
    def loop(loc: Location) {
      f(loc) match {
        case None =>
        case Some(l) => if (filter(l)) b += l; loop(l)
      }
    }

    for (loc <- seed) {
      if (filter(loc)) b += loc
      loop(loc)
    }

    b.result()
  }

  @annotation.tailrec
  def find(start: Option[Location], filter: Filter)(f: Location => Option[Location]): Option[Location] = {
    start match {
      case None => None
      case s @ Some(loc) => if (filter(loc)) s else find(f(loc), filter)(f)
    }
  }

  sealed trait Path

  case object Root extends Path {
    override def toString: String = "ROOT"
  }

  case class Hole(left: List[Node], parent: Location, right: List[Node]) extends Path {
    override def toString: String = "%s/%d".format(parent.path, left.size)
  }

  sealed case class Location(node: Node, path: Path) {
    val id: Int = System.identityHashCode(node)

    protected def create(node: Node, path: Hole): Location = Location(node, path)

    override def equals(other: Any): Boolean = other match {
      case l: Location => l.id == id
      case _ => false
    }

    override def hashCode: Int = id
    
    override def toString: String = "%s at %s".format(node.label, path)
    
    def isTop: Boolean = path == Root

    def isChild: Boolean = !isTop

    def isFirst: Boolean = path match {
      case Root => true
      case Hole(Nil, _, _) => true
      case _ => false
    }
    
    def isLast: Boolean = path match {
      case Root => true
      case Hole(_, _, Nil) => true
      case _ => false
    }

    def previous: Option[Location] = path match {
      case Hole(head :: tail, p, r) => Some(create(head, Hole(tail, p, node :: r)))
      case _ => None
    }
    
    def findPrevious(f: Filter): Option[Location] = find(previous, f)(_.previous)

    def preceding(f: Filter = NoFilter): List[Location] = unfold(previous, f)(_.previous)
    
    def next: Option[Location] = path match {
      case Hole(l, p, head :: tail) => Some(create(head, Hole(node :: l, p, tail)))
      case _ => None
    }
    
    def findNext(f: Filter): Option[Location] = find(next, f)(_.next)

    def following(f: Filter = NoFilter): List[Location] = unfold(next, f)(_.next)

    def first: Location = if (isFirst) this else previous.get.first

    def last: Location = if (isLast) this else next.get.last
    
    def child: Option[Location] = node.child.toList match {
      case Nil => None
      case head :: tail => Some(create(head, Hole(Nil, this, tail)))
    }

    def children(f: Filter = NoFilter): List[Location] = unfold(child, f)(_.next)

    def lastChild: Option[Location] = node.child.reverse match {
      case Nil => None
      case head :: tail => Some(create(head, Hole(tail, this, Nil)))
    }

    def parent: Option[Location] = path match {
      case p: Hole =>
        val xs = p.left.reverse_::: (node :: p.right)
        Some(Location(p.parent.node match {
          case e: Elem => e.copy(child = xs)
          case _: Group => new Group(xs)
        }, p.parent.path))
      case _ => None
    }

    def ancestors(f: Filter = NoFilter): List[Location] = unfold(parent, f)(_.parent)

    def ancestorsOrSelf(f: Filter = NoFilter): List[Location] = this :: ancestors(f)

    def descendants(f: Filter = NoFilter): List[Location] = children(f).flatMap(_.descendantsOrSelf(f))

    def descendantsOrSelf(f: Filter = NoFilter): List[Location] = this :: children(f).flatMap(_.descendantsOrSelf(f))
  }
  
  class ParentLocation(node: Node, path: Hole) extends Location(node, path) {
    override protected def create(node: Node, path: Hole): Location = new ParentLocation(node, path)
    override def parent: Option[Location] = Some(path.parent)
  }
  
  class TopLocation(node: Node) extends Location(node, Root) {
    override protected def create(node: Node, path: Hole): Location = new ParentLocation(node, path)
    override def parent: Option[Location] = None
  }

  def apply(node: Node): Location = new TopLocation(node)
}
