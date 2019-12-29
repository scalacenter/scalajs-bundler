const ScalaJS = require("./scalajs.webpack.config");
const Merge = require("webpack-merge");
const HtmlWebpackPlugin = require("html-webpack-plugin");

const WebApp = Merge(ScalaJS, {
  mode: "development",
  output: {
    filename: "library.js"
  },
  plugins: [new HtmlWebpackPlugin()]
});

module.exports = WebApp;
