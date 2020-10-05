addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "2.6")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "2.0.0")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"    % "2.0.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-site"      % "1.4.0")
addSbtPlugin("com.novocode"      % "sbt-ornate"    % "0.6")
addSbtPlugin("com.eed3si9n"      % "sbt-unidoc"    % "0.4.2")
addSbtPlugin("com.typesafe.sbt"  % "sbt-ghpages"   % "0.6.3")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo" % "0.7.0")

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
