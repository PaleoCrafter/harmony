const passport = require('passport')
const session = require('express-session')
const Strategy = require('passport-discord').Strategy
const Eris = require('eris')
const Permission = Eris.Permission
const Permissions = Eris.Constants.Permissions
const FileStore = require('session-file-store')(session)
const { Channel, PermissionOverride } = require('./db')

const PERMISSIONS_TIMEOUT = 60 * 1000

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

    if (member.permission.allow & Permissions.administrator) {
      return {
        server: member.permission,
        channels: channels.reduce((acc, channel) => ({ ...acc, [channel.id]: new Eris.Permission(Permissions.all) }), {})
      }
    }

    const channelOverrides = (await PermissionOverride.findAll({ where: { channel: channels.map(c => c.id) } }))
      .reduce(
        (acc, override) => {
          return ({ ...acc, [override.channel]: [...(acc[override.channel] || []), override] })
        },
        {}
      )

    const channelPermissions = channels.map((channel) => {
      let permissions = member.permission.allow
      const overrides = channelOverrides[channel.id]

      if (overrides === undefined || overrides.length === 0) {
        return { id: channel.id, permissions: new Eris.Permission(permissions) }
      }

      const serverOverride = overrides.find(override => override.target === serverId)
      if (serverOverride) {
        permissions = (permissions & ~serverOverride.denied) | serverOverride.allowed
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
      permissions = (permissions & ~deny) | allow
      const userOverride = overrides.find(override => override.type === 'USER' && override.target === userId)
      if (userOverride) {
        permissions = (permissions & ~userOverride.denied) | userOverride.allowed
      }
      return { id: channel.id, permissions: new Eris.Permission(permissions) }
    })

    return {
      server: member.permission,
      channels: channelPermissions.reduce((acc, channel) => ({ ...acc, [channel.id]: channel.permissions }), {})
    }
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
      callbackURL: '/api/auth/callback',
      scope: scopes,
      proxy: true
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
      secret: process.env.APPLICATION_SECRET || 'rust and ruin',
      resave: false,
      saveUninitialized: false,
      rolling: true,
      store: new FileStore({}),
      cookie: {
        secure: app.get('env') === 'production',
        maxAge: 30 * 24 * 60 * 60 * 1000 // 30 days
      }
    }))
    app.use(passport.initialize())
    app.use(passport.session())

    app.get(
      '/api/auth/login',
      (req, res, next) => {
        if (req.query.timezone) {
          res.cookie('auth_timezone', req.query.timezone, { maxAge: 900000, httpOnly: true, signed: true })
        }
        if (req.query.redirect) {
          res.cookie('auth_redirect', req.query.redirect, { maxAge: 900000, httpOnly: true, signed: true })
        }
        next()
      },
      passport.authenticate('discord', { scope: scopes })
    )
    app.get(
      '/api/auth/callback',
      passport.authenticate('discord', { failureRedirect: '/' }),
      (req, res) => {
        res.clearCookie('auth_timezone')
        res.clearCookie('auth_redirect')
        req.user.timezone = req.signedCookies.auth_timezone
        res.redirect(req.signedCookies.auth_redirect || '/')
      }
    )
    app.get(
      '/api/auth/logout',
      (req, res) => {
        req.logout()
        res.redirect('/')
      }
    )
  },
  checkAuth (req, res, next) {
    if (req.isAuthenticated()) {
      return next()
    }
    res.status(401)
    res.json({ errors: [{ message: 'Unauthorized' }], data: null })
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

    return {
      server: new Permission(cached.permissions.server.allow, cached.permissions.server.deny),
      channels: Object.keys(cached.permissions.channels).reduce((acc, key) => ({
        ...acc,
        [key]: new Permission(cached.permissions.channels[key].allow, cached.permissions.channels[key].deny)
      }), {})
    }
  }
}
