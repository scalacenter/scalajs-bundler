var uuid = require('uuid');
var config = require('./config.json');

console.log(JSON.stringify(config));

module.exports = {
  someUuid: uuid.v4(),
  someConfig: config
};
