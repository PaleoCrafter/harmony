const path = require('path')
const fs = require('fs')
const graphqlHTTP = require('express-graphql')
const DataLoader = require('dataloader')
const { buildSchema } = require('graphql')
const Op = require('sequelize').Op
const { Server, Channel, User, Message, MessageVersion, UserNickname, Role } = require('./db')
const { checkAuth, getPermissions } = require('./auth')

const schema = buildSchema(fs.readFileSync(path.join(__dirname, 'schema.gqls'), 'utf8'))

function mapColor (color) {
  return {
    r: parseInt(color.substr(0, 2), 16
    ),
    g: parseInt(color.substr(2, 2), 16),
    b: parseInt(color.substr(4, 2), 16)
  }
}

function initLoaders (user) {
  const channelLoader = new DataLoader(async (ids) => {
    const channels = await Channel.findAll({ where: { id: ids } })
    return ids.map(id => channels.find(s => s.id === id) || null)
  })

  function loadServerChannels (server) {
    if (server === undefined) {
      return null
    }

    server.channels = async () => {
      const permissions = (await getPermissions(user, server.id)).channels
      const ids = Object.keys(permissions).filter(id => permissions[id].has('readMessages'))

      return channelLoader.loadMany(ids)
    }

    return server
  }

  const userNicknames = new DataLoader(
    async (keys) => {
      const nicknames = await UserNickname.findAll(
        {
          where: { [Op.and]: { user: keys.map(key => key.id), server: keys.map(key => key.server) } },
          order: [['timestamp', 'DESC']]
        },
        {
          cacheKeyFn: ({ server, id }) => `${server}-${id}`
        }
      )

      return keys.map(({ server, id }) => nicknames.filter(n => n.server === server && n.user === id))
    },
    {
      cacheKeyFn: ({ server, id }) => `${server}-${id}`
    }
  )

  return {
    servers: new DataLoader(async (ids) => {
      const servers = await Server.findAll({ where: { active: true, id: ids } })
      return ids.map(id => loadServerChannels(servers.find(s => s.id === id)))
    }),
    channels: channelLoader,
    users: new DataLoader(
      async (keys) => {
        const users = await User.findAll({ where: { id: keys.map(key => key.id) } })

        function addUserNickname (user, server) {
          if (user === undefined) {
            return null
          }

          return {
            id: user.id,
            name: user.name,
            discriminator: user.discriminator,
            nickname: userNicknames.load({ server, id: user.id }).then(nicks => nicks.map(nick => nick.name)[0] || null)
          }
        }

        return keys.map(({ server, id }) => addUserNickname(users.find(s => s.id === id), server))
      },
      {
        cacheKeyFn: ({ server, id }) => `${server}-${id}`
      }
    ),
    roles: new DataLoader(
      async (keys) => {
        const roles = await Role.findAll({ where: { id: keys.map(key => key.id) } })

        return Promise.all(
          keys.map(async ({ server, id }) => {
            const role = roles.find(r => r.id === id)
            if (role && (role.deletedAt !== null || (await getPermissions(user, server)).server.has('manageRoles'))) {
              return {
                id: role.id,
                name: role.name,
                color: mapColor(role.color),
                deletedAt: role.deletedAt
              }
            }
            return null
          })
        )
      },
      {
        cacheKeyFn: ({ server, id }) => `${server}-${id}`
      }
    ),
    messageVersions: new DataLoader(async (ids) => {
      const versions = await MessageVersion.findAll({
        where: {
          message: ids
        },
        order: [['timestamp', 'DESC']]
      })

      return ids.map(id => versions.filter(v => v.message === id))
    })
  }
}

const root = {
  identity (args, request) {
    const { id, username, discriminator } = request.user
    return { id, name: username, discriminator }
  },
  async servers (args, request) {
    return (await request.loaders.servers.loadMany(request.user.servers.map(server => server.id))).filter(s => s !== null)
  },
  server ({ id: requestedId }, request) {
    return request.user.servers.find(s => s.id === requestedId) ? request.loaders.servers.load(requestedId) : null
  },
  async channel ({ id: requestedId }, request) {
    const channel = await request.loaders.channels.load(requestedId)
    const permissions = (await getPermissions(request.user, channel.server)).channels[channel.id]
    return permissions && permissions.has('readMessages') ? channel : null
  },
  user ({ server, id }, request) {
    return request.user.servers.find(s => s.id === server) ? request.loaders.users.load({ server, id }) : null
  },
  role ({ server, id }, request) {
    return request.user.servers.find(s => s.id === server) ? request.loaders.roles.load({ server, id }) : null
  },
  async messages ({ channel: channelId, before, after, limit }, request) {
    if (limit > 100) {
      throw new Error('You may request at most 100 messages at once!')
    }

    const channel = await request.loaders.channels.load(channelId)
    const permissions = (await getPermissions(request.user, channel.server)).channels[channel.id]

    if (permissions === undefined || !permissions.has('readMessages')) {
      return []
    }

    const messages = await Message.findAll({
      where: {
        channel: channel.id,
        createdAt: {
          [Op.and]: { [Op.gt]: after, [Op.lt]: before }
        },
        ...(
          permissions.has('manageMessages')
            ? {}
            : {
              deletedAt: {
                [Op.ne]: null
              }
            }
        )
      },
      limit,
      order: [['createdAt', 'ASC']]
    })

    return Promise.all(messages.map(async (msg) => {
      const versions = await request.loaders.messageVersions.load(msg.id)
      return {
        author: request.loaders.users.load({ server: msg.server, id: msg.user }),
        server: msg.server,
        versions: permissions.has('manageMessages') ? versions : [versions[0]],
        createdAt: msg.createdAt,
        editedAt: versions.length > 1 ? versions[0].timestamp : null,
        deletedAt: msg.deletedAt
      }
    }))
  }
}

module.exports = {
  init (app) {
    app.use((req, res, next) => {
      req.loaders = initLoaders(req.user)
      next()
    })
    app.use('/api/graphql', checkAuth, graphqlHTTP({
      schema,
      rootValue: root,
      graphiql: true
    }))
  }
}
