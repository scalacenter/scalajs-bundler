const Webpack = require("webpack");

const Test = {
  plugins: [
    new Webpack.DefinePlugin({
      "process.env": {
        NODE_ENV: JSON.stringify("test")
      }
    })
  ]
};

module.exports = Test;
