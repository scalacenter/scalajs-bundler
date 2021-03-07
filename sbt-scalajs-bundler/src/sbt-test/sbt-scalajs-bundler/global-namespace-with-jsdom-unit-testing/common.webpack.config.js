var globalModules = {
  moment: "moment"
};

const importRule = {
  // Force require global modules
  test: /.*-(fast|full)opt\.js$/,
  loader:
    "imports-loader?" +
    Object.keys(globalModules)
      .map(function(modName) {
        return modName + "=" + globalModules[modName];
      })
      .join(",")
};

const exposeRules = Object.keys(globalModules).map(function(modName) {
  // Expose global modules
  return {
    test: require.resolve(modName),
    loader: "expose-loader?" + globalModules[modName]
  };
});

const allRules = exposeRules.concat(importRule);

module.exports = {
  performance: { hints: false },
  module: {
    rules: allRules
  }
};
