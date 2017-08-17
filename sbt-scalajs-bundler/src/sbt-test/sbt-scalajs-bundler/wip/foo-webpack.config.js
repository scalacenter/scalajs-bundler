const path = require('path');
const merge = require("webpack-merge");
const ScalaJsConfig = require('./scalajs.webpack.config');

const RootDir = path.resolve(__dirname, '../../../..');
const FooBarDir = path.resolve(RootDir, 'src/main/foobar');

module.exports = merge(ScalaJsConfig, {
  resolve: {
    alias: {
      'foobar': FooBarDir,
    }
  },
  module: {
    rules: [
      {
        test: /\.(svg)$/,
        loader: 'file-loader',
        options: {
          name: "[name]-loaded.[ext]"
        }
      }
    ]
  }
})