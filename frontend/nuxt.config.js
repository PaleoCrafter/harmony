export default {
  mode: 'universal',
  server: {
    host: '0.0.0.0'
  },
  /*
  ** Headers of the page
  */
  head: {
    htmlAttrs: {
      lang: 'en'
    },
    title: 'Harmony',
    meta: [
      { charset: 'utf-8' },
      { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      { hid: 'description', name: 'description', content: process.env.npm_package_description || '' }
    ],
    link: [
      { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
      { rel: 'preconnect', href: 'https://images-ext-2.discordapp.net' }
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
  plugins: [
    { src: '@/plugins/portal.js' },
    { src: '@/plugins/message-components.js' },
    { src: '@/plugins/client.js', mode: 'client' }
  ],
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
    errorHandler: '@/apollo/error-handler',
    clientConfigs: {
      default: {
        tokenName: 'connect.sid',
        httpEndpoint: 'http://backend:3000/api/graphql',
        browserHttpEndpoint: '/api/graphql'
      },
      identityCheck: {
        tokenName: 'connect.sid',
        httpEndpoint: 'http://backend:3000/api/graphql',
        browserHttpEndpoint: '/api/graphql'
      }
    }
  },
  router: {
    middleware: ['auth']
  },
  build: {
    filenames: {
      app: ({ isDev }) => isDev ? '[name].[hash].js' : '[chunkhash].js',
      chunk: ({ isDev }) => isDev ? '[name].[hash].js' : '[chunkhash].js'
    },
    babel: {
      plugins: ['@babel/plugin-proposal-optional-chaining', '@babel/plugin-proposal-nullish-coalescing-operator']
    }
  }
}
