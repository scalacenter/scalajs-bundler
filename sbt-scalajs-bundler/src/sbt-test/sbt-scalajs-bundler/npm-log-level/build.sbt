name := "npm-log-level"
enablePlugins(ScalaJSBundlerPlugin)
scalaVersion := "2.11.12"
webpackBundlingMode := BundlingMode.LibraryOnly()
emitSourceMaps := false
npmDependencies in Compile += "http-aws-es" -> "6.0.0"

val assertWarning = taskKey[Unit]("checks npm log  warn")
assertWarning := check().value

def check() = Def.task[Unit] {
  val lastLog: File = BuiltinCommands.lastLogFile(state.value).get
  val last: String = IO.read(lastLog)
  val contains = last.contains("[warn] npm")
  if (!contains) {
    sys.error("Expected `[warn] npm`, but not found")
  }
}

