var globalModules = {
  moment: "moment"
};

const importRule = {
  // Force require global modules
  test: /.*-(fast|full)opt\.js$/,
  loader:
    "imports-loader",
  options: {
    type: 'commonjs',
    imports: Object.keys(globalModules)
      .map(function(modName) {
        return {
          moduleName: globalModules[modName],
          name: modName,
        }
      })
  }
};

const exposeRules = Object.keys(globalModules).map(function(modName) {
  // Expose global modules
  return {
    test: require.resolve(modName),
    loader: "expose-loader",
    options: {
      exposes: [globalModules[modName]],
    },
  };
});

const allRules = exposeRules.concat(importRule);

module.exports = {
  performance: { hints: false },
  module: {
    rules: allRules
  }
};
