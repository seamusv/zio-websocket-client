import sbt._

object Dependencies {

  lazy val circe: Seq[ModuleID] = {
    val circeVersion = "0.12.3"

    Seq(
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion
    )
  }

  lazy val logging: Seq[ModuleID] =
    Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )

  lazy val sttp: Seq[ModuleID] = {
    val sttpVersion = "2.0.0-RC6"

    Seq(
      "com.softwaremill.sttp.client" %% "core"                          % sttpVersion,
      "com.softwaremill.sttp.client" %% "circe"                         % sttpVersion,
      "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % sttpVersion
    )
  }

  lazy val zio: Seq[ModuleID] = {
    val zioVersion = "1.0.0-RC17"

    Seq(
      "dev.zio" %% "zio"             % zioVersion,
      "dev.zio" %% "zio-streams"     % zioVersion,
      "dev.zio" %% "zio-macros-core" % "0.6.2",
      "dev.zio" %% "zio-test"        % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt"    % zioVersion % "test"
    )
  }
}
