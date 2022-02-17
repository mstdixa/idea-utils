import org.jetbrains.sbtidea.Keys._

lazy val myAwesomeFramework =
  project.in(file("."))
    .enablePlugins(SbtIdeaPlugin)
    .settings(
      version := "0.0.1-SNAPSHOT",
      scalaVersion := "2.13.2",
      ThisBuild / intellijPluginName := "Dixa Insights Utility",
      ThisBuild / intellijBuild      := "213.5744.223",
      ThisBuild / intellijPlatform   := IntelliJPlatform.IdeaCommunity,
//        ThisBuild / intellijPlatform   := IntelliJPlatform.IdeaUltimate,
      Global    / intellijAttachSources := true,
      Compile / javacOptions ++= "--release" :: "11" :: Nil,
      intellijPlugins += "com.intellij.properties".toPlugin,
      libraryDependencies += "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5" withSources(),
      unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
      unmanagedResourceDirectories in Test    += baseDirectory.value / "testResources",
    )
