module.exports = {
    port: 4201,
    directory: './src/main/resources/static/login',
    spa: 'index.html',
    rewrite: [
        {
            from: '/api/*',
            to: 'http://localhost:8789/api/$1'
        }
    ]
};
