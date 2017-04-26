var merge = require("webpack-merge");

var generatedConfig = require('./scalajs.webpack.config');
var commonConfig = require("./common.webpack.config.js");

module.exports = merge(generatedConfig, commonConfig);