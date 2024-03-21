addSbtPlugin("com.github.sbt"  % "sbt-site"       % "1.6.0")
addSbtPlugin("com.novocode"      % "sbt-ornate"     % "0.6")
addSbtPlugin("com.github.sbt"      % "sbt-unidoc"     % "0.5.0")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo"  % "0.11.0")
addSbtPlugin("com.github.sbt"      % "sbt-ci-release" % "1.5.12")

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
