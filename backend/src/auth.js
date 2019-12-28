const passport = require('passport');
const session = require('express-session');
const Strategy = require('passport-discord').Strategy;
const Eris = require('eris');
const Permissions = Eris.Constants.Permissions;
const FileStore = require('session-file-store')(session);

const PERMISSIONS_TIMEOUT = 60 * 1000;

Object.defineProperty(Eris.Client.prototype, 'getUserPermissions', {
  value: async function getUserPermissions(guildId, userId) {
    if (this.guilds.get(guildId) === undefined) {
      const guild = await this.getRESTGuild(guildId);

      // Dirty hack to make library behave
      guild.shard = {
        client: {
          users: {
            get() {
              return undefined;
            },
            add(user) {
              return user;
            },
          },
        },
      };
      this.guilds.set(guildId, guild);
    }
    const member = await this.getRESTGuildMember(guildId, userId);
    const channels = (await this.getRESTGuildChannels(guildId))
      .filter(channel => channel instanceof Eris.TextChannel || channel instanceof Eris.NewsChannel);

    let permission = member.permission.allow;
    if (permission & Permissions.administrator) {
      return channels.reduce((acc, channel) => ({ ...acc, [channel.id]: new Eris.Permission(Permissions.all) }), {});
    }

    const channelPermissions = {};
    channels.forEach(channel => {
      let overwrite = channel.permissionOverwrites.get(guildId);
      if (overwrite) {
        permission = (permission & ~overwrite.deny) | overwrite.allow;
      }
      let deny = 0;
      let allow = 0;
      for (const roleID of member.roles) {
        if ((overwrite = channel.permissionOverwrites.get(roleID))) {
          deny |= overwrite.deny;
          allow |= overwrite.allow;
        }
      }
      permission = (permission & ~deny) | allow;
      overwrite = channel.permissionOverwrites.get(userId);
      if (overwrite) {
        permission = (permission & ~overwrite.deny) | overwrite.allow;
      }
      return new Eris.Permission(permission);
    });

    return channelPermissions;
  },
});

const scopes = ['identify', 'guilds'];

module.exports = {
  init(app) {
    const discordClient = new Eris(`Bot ${process.env.DISCORD_BOT_TOKEN}`, { restMode: true });

    passport.serializeUser(function (user, done) {
      done(null, user);
    });
    passport.deserializeUser(function (obj, done) {
      done(null, obj);
    });

    passport.use(new Strategy({
      clientID: process.env.DISCORD_CLIENT_ID,
      clientSecret: process.env.DISCORD_CLIENT_SECRET,
      callbackURL: 'http://localhost:3000/auth/callback',
      scope: scopes,
    }, function (accessToken, refreshToken, profile, done) {
      const user = { username: profile.username, discriminator: profile.discriminator, servers: profile.guilds, permissionsCache: {} };
      user.getPermissions = (server) => {
        let cached = user.permissionsCache[server];
        const now = Date.now();
        if (cached === undefined || (now - cached.lastCheck) >= PERMISSIONS_TIMEOUT) {
          cached = {
            permissions: discordClient.getUserPermissions(server, user.id),
            lastCheck: now,
          };
        }

        return cached.permissions;
      };
      done(null, user);
    }));

    app.use(session({
      secret: process.env.SESSION_SECRET || 'rust and ruin',
      resave: false,
      saveUninitialized: false,
      store: new FileStore({}),
    }));
    app.use(passport.initialize());
    app.use(passport.session());

    app.get('/auth/login', passport.authenticate('discord', { scope: scopes }));
    app.get('/auth/callback', passport.authenticate('discord', { successRedirect: '/success', failureRedirect: '/failed' }));
    app.get('/auth/logout', function (req, res) {
      req.logout();
      res.redirect('/');
    });
  },
  checkAuth(req, res, next) {
    if (req.isAuthenticated()) return next();
    res.redirect('/auth/login');
  },
};
