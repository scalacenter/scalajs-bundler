libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.eed3si9n"      % "sbt-unidoc"   % "0.3.3")
addSbtPlugin("com.github.gseitz" % "sbt-release"  % "1.0.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"      % "1.0.0")
addSbtPlugin("com.novocode"      % "sbt-ornate"   % "0.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-ghpages"  % "0.5.4")
addSbtPlugin("com.typesafe.sbt"  % "sbt-site"     % "1.1.0")
addSbtPlugin("io.get-coursier"   % "sbt-coursier" % "1.0.0-RC10")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype" % "1.1")