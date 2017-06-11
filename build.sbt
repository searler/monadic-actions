scalaVersion in ThisBuild := "2.11.8"

organization in ThisBuild := "searler"

description := "Mini DSL to allow the writing of  actions using for-comprehensions"

licenses in ThisBuild += ("Apache2", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))


scalacOptions in ThisBuild ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-encoding", "utf8",
  "-Xfatal-warnings"
)

val commonSettings = Seq (
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % "1.13.0" % "test",
    "org.specs2" %% "specs2-core" % "3.7" % "test"
  )

)

lazy val support = (project in file("support"))
  .settings(commonSettings:_*)
  .settings(name := "monadic-actions-support")


lazy val core = (project in file("core"))
  .settings(commonSettings:_*)
  .settings(name := "monadic-actions")
  .dependsOn(support)

lazy val cats = (project in file("cats"))
  .settings(commonSettings:_*)
  .settings(
    name := "monadic-actions-cats",
    libraryDependencies ++= Seq("org.typelevel" %% "cats" % "0.7.2" % "provided")
  )
  .dependsOn(core,support)




