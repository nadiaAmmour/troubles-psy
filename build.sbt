name := "troubles-psy1"
version := "1.0.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "com.github.tototoshi"     %% "scala-csv"        % "1.3.10",
  "ch.qos.logback"            % "logback-classic"  % "1.4.11",
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.5",
  "org.scalatest"            %% "scalatest"        % "3.2.17" % Test,
  "com.github.mjakubowski84" %% "parquet4s-core"   % "2.14.0",
  "org.apache.hadoop"         % "hadoop-client"    % "3.3.6"
)

assembly / mainClass := Some("eeg.Main")
assembly / assemblyJarName := "ms1-eeg.jar"