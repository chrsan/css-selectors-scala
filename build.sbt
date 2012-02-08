organization := "se.fishtank"

name := "css-selectors-scala"

version := "0.1.0"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xcheckinit", "-Xmigration", "-encoding", "UTF-8")

publishTo := Some(Resolver.file("Github Pages", Path.userHome / "Development" / "Source" / "Misc" / "chrsan.github.com" / "maven" asFile)(Patterns(true, Resolver.mavenStyleBasePattern)))

publishMavenStyle := true

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.6.1" % "test"
)

