const ScalaJS = require("./scalajs.webpack.config");
const Merge = require("webpack-merge");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const path = require("path");
const rootDir = path.resolve(__dirname, "../../../..");
const resourcesDir = path.resolve(rootDir, "src/main/resources");
// LessLoader is requested but it is missing from npmDevDependencies
const LessLoaderPlugin = require("less-loader");

const WebApp = Merge(ScalaJS, {
  mode: "development",
  entry: {
    app: [path.resolve(resourcesDir, "./entry.js")]
  },
  plugins: [new HtmlWebpackPlugin()]
});

module.exports = WebApp;
