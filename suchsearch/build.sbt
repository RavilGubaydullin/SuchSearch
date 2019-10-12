name := """SuchSearch"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1",
  "org.apache.kafka" % "kafka_2.10" % "0.9.0.1",
  "com.thenewmotion" %% "akka-rabbitmq" % "3.0.0",
  "org.mongodb" %% "casbah" % "3.1.1",
  "com.github.haifengl" % "smile-core" % "1.0.2",
  "info.debatty" % "java-string-similarity" % "0.13"
)

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "The New Motion Public Repo" at "http://nexus.thenewmotion.com/content/groups/public/"
)
