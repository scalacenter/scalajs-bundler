addSbtPlugin("com.typesafe.sbt"  % "sbt-site"       % "1.4.1")
addSbtPlugin("com.novocode"      % "sbt-ornate"     % "0.6")
addSbtPlugin("com.github.sbt"      % "sbt-unidoc"     % "0.5.0")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo"  % "0.10.0")
addSbtPlugin("com.geirsson"      % "sbt-ci-release" % "1.5.6")

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
