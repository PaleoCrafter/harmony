const passport = require('passport')
const session = require('express-session')
const Strategy = require('passport-discord').Strategy
const Eris = require('eris')
const Permissions = Eris.Constants.Permissions
const FileStore = require('session-file-store')(session)
const { Channel } = require('./db')

const PERMISSIONS_TIMEOUT = 1

Object.defineProperty(Eris.Client.prototype, 'getUserPermissions', {
  value: async function getUserPermissions (serverId, userId) {
    if (this.guilds.get(serverId) === undefined) {
      const guild = await this.getRESTGuild(serverId)

      // Dirty hack to make library behave
      guild.shard = {
        client: {
          users: {
            get () {
              return undefined
            },
            add (user) {
              return user
            }
          }
        }
      }
      this.guilds.set(serverId, guild)
    }
    const member = await this.getRESTGuildMember(serverId, userId)
    const channels = await Channel.findAll({ where: { server: serverId } })

    let permission = member.permission.allow
    if (permission & Permissions.administrator) {
      return channels.reduce((acc, channel) => ({ ...acc, [channel.id]: new Eris.Permission(Permissions.all) }), {})
    }

    const channelPermissions = await Promise.all(channels.map(async (channel) => {
      const overrides = await channel.getPermissionOverrides()
      const serverOverride = overrides.find(override => override.target === serverId)
      if (serverOverride) {
        permission = (permission & ~serverOverride.denied) | serverOverride.allowed
      }
      let deny = 0
      let allow = 0
      for (const roleId of member.roles) {
        const roleOverride = overrides.find(override => override.type === 'ROLE' && override.target === roleId)
        if (roleOverride) {
          deny |= roleOverride.denied
          allow |= roleOverride.allowed
        }
      }
      permission = (permission & ~deny) | allow
      const userOverride = overrides.find(override => override.type === 'USER' && override.target === userId)
      if (userOverride) {
        permission = (permission & ~userOverride.denied) | userOverride.allowed
      }
      return { id: channel.id, permissions: new Eris.Permission(permission) }
    }))

    return channelPermissions.reduce((acc, channel) => ({ ...acc, [channel.id]: channel.permissions }), {})
  }
})

const scopes = ['identify', 'guilds']

module.exports = {
  init (app) {
    this.discordClient = new Eris(`Bot ${process.env.DISCORD_BOT_TOKEN}`, { restMode: true })

    passport.serializeUser(function (user, done) {
      done(null, user)
    })
    passport.deserializeUser(function (obj, done) {
      done(null, obj)
    })

    passport.use(new Strategy({
      clientID: process.env.DISCORD_CLIENT_ID,
      clientSecret: process.env.DISCORD_CLIENT_SECRET,
      callbackURL: 'http://localhost:9000/api/auth/callback',
      scope: scopes
    }, function (accessToken, refreshToken, profile, done) {
      done(null, {
        id: profile.id,
        username: profile.username,
        discriminator: profile.discriminator,
        servers: profile.guilds,
        permissionsCache: {}
      })
    }))

    app.use((req, res, next) => {
      const authHeader = req.header('authorization')
      if (authHeader && authHeader.startsWith('Session ')) {
        req.signedCookies['connect.sid'] = authHeader.substring(8)
      }
      next()
    })
    app.use(session({
      secret: process.env.SESSION_SECRET || 'rust and ruin',
      resave: false,
      saveUninitialized: false,
      store: new FileStore({})
    }))
    app.use(passport.initialize())
    app.use(passport.session())

    app.get('/api/auth/login', passport.authenticate('discord', { scope: scopes }))
    app.get('/api/auth/callback', passport.authenticate('discord', { successRedirect: '/api/success', failureRedirect: '/api/failed' }))
    app.get('/api/auth/logout', function (req, res) {
      req.logout()
      res.redirect('/')
    })
  },
  checkAuth (req, res, next) {
    if (req.isAuthenticated()) {
      return next()
    }
    res.redirect('/api/auth/login')
  },
  async getPermissions (user, server) {
    let cached = user.permissionsCache[server]
    const now = Date.now()
    if (cached === undefined || (now - cached.lastCheck) >= PERMISSIONS_TIMEOUT) {
      cached = {
        permissions: await this.discordClient.getUserPermissions(server, user.id),
        lastCheck: now
      }
      user.permissionsCache[server] = cached
    }

    return cached.permissions
  }
}
