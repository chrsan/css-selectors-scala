organization := "se.fishtank"

name := "css-selectors-scala"

version := "0.1.3"

scalaVersion := "2.10.0"

crossScalaVersions := Seq("2.10.0", "2.9.2", "2.9.1")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xcheckinit", "-Xmigration", "-encoding", "UTF-8")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <url>http://github.com/chrsan/css-selectors-scala</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:chrsan/css-selectors-scala.git</url>
    <connection>scm:git:git@github.com:chrsan/css-selectors-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>chrsan</id>
      <name>Christer Sandberg</name>
      <url>http://chrsan.github.com</url>
    </developer>
  </developers>)
