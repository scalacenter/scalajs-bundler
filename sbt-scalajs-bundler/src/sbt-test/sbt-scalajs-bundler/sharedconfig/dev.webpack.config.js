var merge = require("webpack-merge")
var commonConfig = require("./common.webpack.config.js")

module.exports = merge(commonConfig, {

  "entry": {
    "sharedconfig-fastopt": "./fastopt-launcher.js"
  },
  "output": {
    "filename": "[name]-bundle.js"
  },
  "devtool": "source-map",
  "module": {
    "preLoaders": [{
      "test": new RegExp("\\.js$"),
      "loader": "source-map-loader"
    }]
  }
})