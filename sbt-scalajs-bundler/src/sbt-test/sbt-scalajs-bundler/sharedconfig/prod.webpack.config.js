var webpack = require('webpack');
var merge = require("webpack-merge")

var commonConfig = require("./common.webpack.config.js")

module.exports = merge(commonConfig, {

  "entry": {
    "sharedconfig-opt": "./opt-launcher.js"
  },
  "output": {
    "filename": "[name]-bundle.js"
  },
  "plugins": [
    new webpack.optimize.UglifyJsPlugin()
  ]
})