const config = require('./lws.config');

config.rewrite[0].to = 'http://localhost:8080/api/$1';
//config.rewrite[0].to = 'http://10.4.0.5:30560/$1';
// config.rewrite[0].to = 'https://10.4.0.5:32172/$1';

module.exports = config;
