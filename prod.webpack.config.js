var webpack = require('webpack');
var scalajsConfig = require('./scalajs-webpack-config');

module.exports = {
  entry: scalajsConfig.entry,
  output: {
    path: scalajsConfig.output.path,
    filename: scalajsConfig.output.filename
  },
  plugins: [
    new webpack.optimize.UglifyJsPlugin()
  ]
};