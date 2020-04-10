lazy val projectCodename = "pythia"

name := projectCodename
organization in ThisBuild := "org.kys"
version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.13.1"

// Projects
lazy val global = project
  .in(file("."))
  .settings(settings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(backend)

lazy val backend = project
  .settings(name := "backend",
    settings,
    assemblySettings,
    libraryDependencies ++=
      Seq(dependencies.catsCore,
        dependencies.catsEffect,
        dependencies.typesafeConfig,
        dependencies.pureconfig,
        dependencies.pureconfigCats,
        dependencies.logbackClassic,
        dependencies.scalaLogging,
        dependencies.http4sDsl,
        dependencies.http4sBlazeServer,
        dependencies.http4sBlazeClient,
        dependencies.http4sCirce,
        dependencies.rhoSwagger,
        dependencies.circeGeneric,
        dependencies.circeGenericExtras,
        dependencies.circeParser,
        dependencies.circeLiteral,
        dependencies.enumeratum,
        dependencies.enumeratumCirce,
        dependencies.sttpCore,
        dependencies.sttpCirce,
        dependencies.sttpCats,
        dependencies.mailgun4s,
        dependencies.flyway,
        dependencies.doobieCore,
        dependencies.doobiePostgres,
        dependencies.doobieHikari,
        dependencies.postgresJdbc),
    javaOptions in Compile ++= Seq("-J-Xss8M"))

// Settings
lazy val compilerOptions = Seq("-unchecked", "-feature", "-deprecation", "-Wunused:imports", "-Ymacro-annotations",
  "-encoding", "utf8")

lazy val commonSettings = Seq(scalacOptions ++= compilerOptions,
  resolvers += "jitpack" at "https://jitpack.io")

lazy val wartremoverSettings = Seq(wartremoverWarnings in(Compile, compile) ++= Warts.unsafe.filterNot { w =>
  w == Wart.Any || w == Wart.Nothing || w == Wart.DefaultArguments || w == Wart.StringPlusAny ||
    w == Wart.NonUnitStatements
})

lazy val assemblySettings = Seq(assemblyJarName in assembly := projectCodename + "-" + name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x if x.endsWith("module-info.class") => MergeStrategy.discard
    case "application.conf" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  })

lazy val settings = commonSettings ++ wartremoverSettings

// Dependencies repository
lazy val dependencies = new {
  val catsVersion = "2.0.0"
  val http4sVersion = "0.21.3"
  val rhoVersion = "0.20.0"
  val circeVersion = "0.13.0"
  val enumeratumVersion = "1.5.13"
  val sttpVersion = "2.0.7"
  val doobieVersion = "0.8.8"
  val flywayVersion = "6.3.1"
  val pureconfigVersion = "0.12.2"

  val catsCore = "org.typelevel" %% "cats-core" % catsVersion
  val catsEffect = "org.typelevel" %% "cats-effect" % catsVersion

  val typesafeConfig = "com.typesafe" % "config" % "1.4.0"
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % pureconfigVersion
  val pureconfigCats = "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfigVersion

  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  val http4sDsl = "org.http4s" %% "http4s-dsl" % http4sVersion
  val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
  val http4sBlazeClient = "org.http4s" %% "http4s-blaze-client" % http4sVersion
  val http4sCirce = "org.http4s" %% "http4s-circe" % http4sVersion
  val rhoSwagger = "org.http4s" %% "rho-swagger" % rhoVersion

  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val circeLiteral = "io.circe" %% "circe-literal" % circeVersion

  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion
  val enumeratumCirce = "com.beachape" %% "enumeratum-circe" % "1.5.22"

  val sttpCore = "com.softwaremill.sttp.client" %% "core" % sttpVersion
  val sttpCirce = "com.softwaremill.sttp.client" %% "circe" % sttpVersion
  val sttpCats = "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % sttpVersion

  val mailgun4s = "org.matthicks" %% "mailgun4s" % "1.0.13"
  val flyway = "org.flywaydb" % "flyway-core" % flywayVersion
  val doobieCore = "org.tpolecat" %% "doobie-core" % doobieVersion
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % doobieVersion
  val postgresJdbc = "org.postgresql" % "postgresql" % "42.2.11"
}
