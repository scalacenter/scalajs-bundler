var merge = require('webpack-merge');

var commonConfig = require('./common.webpack.config');
var generatedConfig = require('./scalajs.webpack.config');

module.exports = merge(generatedConfig, commonConfig);