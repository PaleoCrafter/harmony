const express = require('express')
const cookieParser = require('cookie-parser')
const logger = require('morgan')

const { init: initAuth } = require('./auth')
const { init: initGraphQL } = require('./graphql')

const app = express()

app.set('trust proxy', true)
app.use(logger('dev'))
app.use(express.json())
app.use(express.urlencoded({ extended: false }))
app.use(cookieParser(process.env.APPLICATION_SECRET || 'secret'))
initAuth(app)
initGraphQL(app)

module.exports = app
