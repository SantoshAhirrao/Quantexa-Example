module.exports = {
  port: 4200,
  directory: './dist',
  spa: 'index.html',
  rewrite: [
      {
          from: '/api/*',
          to: 'http://10.4.0.5:30955/api/$1'
      },
      {
          from: '/api-root/*',
          to: `http://10.4.0.5:30955/api-root/$1`
      },
      {
          from: '/login*',
          to: 'http://localhost:4201$1'
      }
  ]
};
