var globalModules = {
  moment: "moment"
};

var config = {
  module: {
    loaders: [
      {
        // Force require global modules
        test: /.*-(fast|full)opt\.js$/,
        loader: "imports-loader?" + Object.keys(globalModules).map(function(modName) {
          return modName + "=" + globalModules[modName];
        }).join(',')
      }
    ]
  }
}

Object.keys(globalModules).forEach(function(modName) {
  // Expose global modules
  config.module.rules.push(
    {
      test: require.resolve(modName),
      loader: "expose-loader?" + globalModules[modName]
    }
  );
});


module.exports = config;