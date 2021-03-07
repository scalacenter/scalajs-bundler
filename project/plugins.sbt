addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "2.3")
addSbtPlugin("com.github.sbt"      % "sbt-pgp"       % "2.1.2")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"    % "2.1.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-site"      % "1.4.1")
addSbtPlugin("com.novocode"      % "sbt-ornate"    % "0.6")
addSbtPlugin("com.eed3si9n"      % "sbt-unidoc"    % "0.4.3")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo" % "0.10.0")

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
