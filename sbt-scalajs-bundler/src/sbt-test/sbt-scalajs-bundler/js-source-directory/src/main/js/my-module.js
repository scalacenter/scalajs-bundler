var uuid = require('uuid');
var config = require('./config.json');
var nestedConfig = require('./nested/config2.json');

console.log(JSON.stringify(config));
console.log(JSON.stringify(nestedConfig));

module.exports = {
  someUuid: uuid.v4(),
  someConfig: config,
  someNestedConfig: nestedConfig
};
