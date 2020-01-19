lazy val root = project
  .in(file("."))
  .settings(BuildHelper.stdSettings("zio-websocket-client"))
  .settings(libraryDependencies ++= Dependencies.zio ++ Dependencies.circe ++ Dependencies.sttp ++ Dependencies.logging)
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .enablePlugins(JavaAppPackaging)
