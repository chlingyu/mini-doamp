module.exports = {
  devServer: {
    port: 8090,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true
      }
    }
  }
};