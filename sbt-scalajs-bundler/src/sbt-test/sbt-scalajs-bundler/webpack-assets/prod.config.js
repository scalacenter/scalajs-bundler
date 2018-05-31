const ScalaJS = require("./scalajs.webpack.config");
const Merge = require("webpack-merge");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const path = require("path");
const rootDir = path.resolve(__dirname, "../../../..");
const resourcesDir = path.resolve(rootDir, "src/main/resources");

const WebApp = Merge(ScalaJS, {
  mode: "production",
  entry: {
    app: [path.resolve(resourcesDir, "./entry.js")]
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: ["style-loader", MiniCssExtractPlugin.loader, "css-loader"]
      }
    ]
  },
  output: {
    filename: "[name].[chunkhash].js",
    path: path.resolve(rootDir, "demo")
  },
  plugins: [new HtmlWebpackPlugin(), new MiniCssExtractPlugin({})]
});

module.exports = WebApp;
