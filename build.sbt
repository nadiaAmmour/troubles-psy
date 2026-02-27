name := "troubles-psy1"
version := "1.0.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv"           % "1.3.10",
  "io.circe"             %% "circe-core"           % "0.14.6",
  "io.circe"             %% "circe-generic"        % "0.14.6",
  "io.circe"             %% "circe-parser"         % "0.14.6",
  "ch.qos.logback"        % "logback-classic"      % "1.4.11",
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.5",
  "org.scalatest"        %% "scalatest"            % "3.2.17" % Test

)

assembly / mainClass := Some("eeg.Main")
assembly / assemblyJarName := "ms1-eeg.jar"
