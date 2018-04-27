const ScalaJS = require("./scalajs.webpack.config");
const Merge = require("webpack-merge");
const HtmlWebpackPlugin = require("html-webpack-plugin");
// LessLoader is requested but it is missing from npmDevDependencies
const LessLoaderPlugin = require("less-loader");

const WebApp = Merge(ScalaJS, {
  mode: "development",
  output: {
    filename: "library.js"
  },
  plugins: [new HtmlWebpackPlugin()]
});

module.exports = WebApp;
