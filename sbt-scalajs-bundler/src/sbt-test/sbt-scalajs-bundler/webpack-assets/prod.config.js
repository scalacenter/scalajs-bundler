const ScalaJS = require("./scalajs.webpack.config");
const Merge = require("webpack-merge");
const HtmlWebpackPlugin = require("html-webpack-plugin");

const path = require("path");
const rootDir = path.resolve(__dirname, "../../../..");

const WebApp = Merge(ScalaJS, {
  mode: "production",
  output: {
    filename: "app.js",
    path: path.resolve(rootDir, "demo")
  },
  plugins: [new HtmlWebpackPlugin()]
});

module.exports = WebApp;
