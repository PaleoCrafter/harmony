const express = require('express')
const cookieParser = require('cookie-parser')
const logger = require('morgan')

const { init: initAuth, checkAuth, getPermissions } = require('./auth')
const { init: initGraphQL } = require('./graphql')

const app = express()

app.use(logger('dev'))
app.use(express.json())
app.use(express.urlencoded({ extended: false }))
app.use(cookieParser(process.env.APPLICATION_SECRET || 'secret'))
initAuth(app)
initGraphQL(app)

app.get('/api', checkAuth, async function (req, res) {
  res.json(await getPermissions(req.user, '606562167407378442'))
})
app.get('/api/success', function (req, res) {
  res.json({ message: 'it worked' })
})
app.get('/api/failed', function (req, res) {
  res.json({ message: 'shit' })
})

module.exports = app
