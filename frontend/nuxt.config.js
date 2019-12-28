export default {
  mode: 'universal',
  server: {
    host: '0.0.0.0'
  },
  /*
  ** Headers of the page
  */
  head: {
    title: 'Harmony',
    meta: [
      { charset: 'utf-8' },
      { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      { hid: 'description', name: 'description', content: process.env.npm_package_description || '' }
    ],
    link: [
      { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }
    ]
  },
  /*
  ** Customize the progress-bar color
  */
  loading: { color: '#7289DA' },
  /*
  ** Global CSS
  */
  css: [],
  /*
  ** Plugins to load before mounting the App
  */
  plugins: [],
  /*
  ** Nuxt.js dev-modules
  */
  buildModules: [
    '@nuxtjs/eslint-module'
  ],
  /*
  ** Nuxt.js modules
  */
  modules: [
    '@nuxtjs/axios',
    '@nuxtjs/apollo'
  ],
  apollo: {
    authenticationType: 'Session',
    clientConfigs: {
      default: {
        tokenName: 'connect.sid',
        httpEndpoint: 'http://backend:3000/api/graphql',
        browserHttpEndpoint: '/api/graphql'
      }
    }
  },
  build: {
    filenames: {
      app: ({ isDev }) => isDev ? '[name].[hash].js' : '[chunkhash].js',
      chunk: ({ isDev }) => isDev ? '[name].[hash].js' : '[chunkhash].js'
    }
  }
}
