name := "zio-websocket-client"

version := "0.1"

scalaVersion := "2.13.1"

scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-Xlint:_,-type-parameter-shadow",
  "-Xsource:2.13",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings",
  "-Wunused:imports",
  "-Wvalue-discard",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wunused:params",
  "-Wvalue-discard",
  "-Wdead-code"
)

libraryDependencies ++= {
  val sttpVersion = "2.0.0-RC5"
  val zioVersion  = "1.0.0-RC17"

  Seq(
    "dev.zio"                      %% "zio"                           % zioVersion,
    "dev.zio"                      %% "zio-streams"                   % zioVersion,
    "com.softwaremill.sttp.client" %% "core"                          % sttpVersion,
    "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % sttpVersion,
    "ch.qos.logback"               % "logback-classic"                % "1.2.3"
  )
}

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
