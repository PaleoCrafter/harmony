const Eris = require('eris');
const Permissions = Eris.Constants.Permissions;
const express = require('express');
const cookieParser = require('cookie-parser');
const logger = require('morgan');

const { init: initAuth } = require('./auth');
const { init: initGraphQL } = require('./graphql');

const app = express();

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
initAuth(app);
initGraphQL(app);

app.get('/', function (req, res) {
  const client = new Eris(`Bot ${process.env.DISCORD_BOT_TOKEN}`, { restMode: true });
  client.getUserPermissions('606562167407378442', '176844651142184963').then(result => {
    res.json(result);
  });
});
app.get('/success', function (req, res) {
  res.json({ 'message': 'it worked' });
});
app.get('/failed', function (req, res) {
  res.json({ 'message': 'shit' });
});

module.exports = app;
