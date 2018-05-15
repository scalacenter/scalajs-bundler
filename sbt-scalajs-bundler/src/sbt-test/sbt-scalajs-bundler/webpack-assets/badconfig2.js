const ScalaJS = require("./scalajs.webpack.config");
const Merge = require("webpack-merge");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const path = require("path");
const rootDir = path.resolve(__dirname, "../../../..");
const resourcesDir = path.resolve(rootDir, "src/main/resources");

const WebApp = Merge(ScalaJS, {
  mode: "development",
  entry: {
    app: [path.resolve(resourcesDir, "./entry.js")]
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: ["style-loader"] // We should include css-loader
      }
    ]
  },
  plugins: [new HtmlWebpackPlugin()]
});

module.exports = WebApp;
