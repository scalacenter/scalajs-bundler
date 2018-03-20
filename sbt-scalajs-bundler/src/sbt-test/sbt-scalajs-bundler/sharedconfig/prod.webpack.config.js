var webpack = require("webpack");
var merge = require("webpack-merge");

var generatedConfig = require("./scalajs.webpack.config");
var commonConfig = require("./common.webpack.config.js");
var UglifyJsPlugin = require("uglifyjs-webpack-plugin");

module.exports = merge(generatedConfig, commonConfig, {
  plugins: [
    new UglifyJsPlugin({
      sourceMap: true
    })
  ]
});
