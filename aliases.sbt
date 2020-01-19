import ProjectUtil._

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("dist", "; clean; compile; test; universal:packageBin")
addCommandAlias("up2date", "reload plugins; dependencyUpdates; reload return; dependencyUpdates")

onLoadMessage +=
  s"""|
      |───────────────────────────
      |  List of defined ${styled("aliases")}
      |────────┬──────────────────
      |${styled("fmt")}     │ project
      |${styled("check")}   │ compile
      |${styled("dist")}    │ compile all
      |${styled("up2date")} │ dependencyUpdates
      |────────┴──────────────────""".stripMargin
