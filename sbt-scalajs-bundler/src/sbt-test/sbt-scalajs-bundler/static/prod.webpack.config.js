var UglifyJsPlugin = require('uglifyjs-webpack-plugin');

module.exports = require('./scalajs.webpack.config');

module.exports.plugins = (module.exports.plugins || []).concat([
  new UglifyJsPlugin({ sourceMap: module.exports.devtool === 'source-map' })
]);
