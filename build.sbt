name := "gitbucket-fess-plugin"

organization := "org.codelibs.gitbucket"

version := "1.3.1-SNAPSHOT"

scalaVersion := "2.12.4"

gitbucketVersion := "4.20.0"

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

scalacOptions := Seq("-deprecation")

javacOptions in compile ++= Seq("-source",
                                "1.8",
                                "-target",
                                "1.8",
                                "-encoding",
                                "UTF-8")

publishArtifact in Test := false

pomIncludeRepository := { _ =>
  false
}

pomExtra := (
  <url>https://github.com/codelibs/gitbucket-fess-plugin</url>
    <licenses>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/codelibs/gitbucket-fess-plugin</url>
      <connection>scm:git:https://github.com/codelibs/gitbucket-fess-plugin.git</connection>
    </scm>
    <developers>
      <developer>
        <id>codelibs-team</id>
        <name>CodeLibs Team</name>
      </developer>
    </developers>
)
