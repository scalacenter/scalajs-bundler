const ScalaJS = require("./scalajs.webpack.config");
const { merge } = require("webpack-merge");
const HtmlWebpackPlugin = require("html-webpack-plugin");

const WebApp = merge(ScalaJS, {
  output: {
    filename: "library.js"
  },
  plugins: [new HtmlWebpackPlugin()]
});

module.exports = WebApp;
