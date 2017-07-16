var globalModules = {
  moment: "moment"
};

module.exports = {
  module: {
    loaders: [
      {
        // Force require global modules
        test: /.*-(fast|full)opt\.js$/,
        loader: "imports-loader?" + Object.keys(globalModules).map(function(modName) {
          return modName + "=" + globalModules[modName];
        }).join(',')
      },
      Object.keys(globalModules).map(function(modName) {
        // Expose global modules
        return {
          test: require.resolve(modName),
          loader: "expose-loader?" + globalModules[modName]
        }
      })
    ]
  }
};