const path = require('path')
const fs = require('fs')
const graphqlHTTP = require('express-graphql')
const DataLoader = require('dataloader')
const { buildSchema } = require('graphql')
const Sequelize = require('sequelize')
const { Op, QueryTypes } = Sequelize
const { database, Server, Channel, User, Message, MessageVersion, Role, Embed, EmbedField } = require('./db')
const { checkAuth, getPermissions } = require('./auth')

const schema = buildSchema(fs.readFileSync(path.join(__dirname, 'schema.gqls'), 'utf8'))

function mapColor (color) {
  return {
    r: parseInt(color.substr(0, 2), 16),
    g: parseInt(color.substr(2, 2), 16),
    b: parseInt(color.substr(4, 2), 16)
  }
}

function embedEntry (embed, prefix, fields) {
  let nonNull = false
  const obj = {}
  fields.forEach((f) => {
    const capitalized = f.charAt(0).toUpperCase() + f.slice(1)
    const value = embed[`${prefix}${capitalized}`]
    obj[f] = value
    nonNull |= value !== null
  })
  return nonNull ? obj : null
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

      return (await channelLoader.loadMany(ids)).filter(channel => channel !== null).sort((a, b) => a.position - b.position)
    }

    return server
  }

  const userNicknames = new DataLoader(
    async (keys) => {
      const variables = keys.map(({ server, id }, index) => ({
        query: `($server${index}::bigint, $user${index}::bigint)`,
        values: {
          [`server${index}`]: server,
          [`user${index}`]: id
        }
      }))
      const nicknames = await database.query(
        `
        SELECT
          "usernicknames"."server" AS "server",
          "usernicknames"."user" AS "user",
          "usernicknames"."name" AS "name",
          "usernicknames"."timestamp" AS "timestamp"
        FROM "usernicknames"
        JOIN (VALUES ${variables.map(v => v.query).join(', ')}) AS conditions (s, u)
          ON "usernicknames"."server" = s AND "usernicknames"."user" = u
        ORDER BY "usernicknames"."timestamp" DESC
        `,
        {
          bind: variables.reduce((acc, v) => ({ ...acc, ...v.values }), {}),
          type: QueryTypes.SELECT
        }
      )

      return keys.map(({ server, id }) => nicknames.filter(n => n.server === server && n.user === id))
    },
    {
      cacheKeyFn: ({ server, id }) => `${server}-${id}`
    }
  )

  const roles = new DataLoader(
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
  )

  const userRoles = new DataLoader(
    async (keys) => {
      const variables = keys.map(({ server, id }, index) => ({
        query: `($server${index}::bigint, $user${index}::bigint)`,
        values: {
          [`server${index}`]: server,
          [`user${index}`]: id
        }
      }))
      const roles = await database.query(
        `
        SELECT
          "userroles"."server" AS "server",
          "userroles"."user" AS "user",
          "roles"."name" AS "name",
          "roles"."color" AS "color",
          "roles"."position" AS "position",
          "roles"."deletedAt" AS "deletedAt"
        FROM "userroles"
        JOIN "roles" ON "roles"."id" = "userroles"."role"
        JOIN (VALUES ${variables.map(v => v.query).join(', ')}) AS conditions (s, u)
          ON "userroles"."server" = s AND "userroles"."user" = u
        ORDER BY "roles"."position" DESC
        `,
        {
          bind: variables.reduce((acc, v) => ({ ...acc, ...v.values }), {}),
          type: QueryTypes.SELECT
        }
      )

      return keys.map(({ server, id }) => {
        const userRoles = roles.filter(r => r.server === server && r.user === id)
        return userRoles.map(role => ({
          id: role.id,
          name: role.name,
          color: mapColor(role.color),
          deletedAt: role.deletedAt
        }))
      })
    },
    {
      cacheKeyFn: ({ server, id }) => `${server}-${id}`
    }
  )

  const embedFields = new DataLoader(async (ids) => {
    const fields = await EmbedField.findAll({ where: { embed: ids }, order: [['position', 'ASC']] })
    return ids.map(id => fields.filter(f => f.embed === id))
  })

  return {
    servers: new DataLoader(async (ids) => {
      const servers = await Server.findAll({ where: { active: true, id: ids } })
      return ids.map(id => loadServerChannels(servers.find(s => s.id === id)))
    }),
    channels: channelLoader,
    users: new DataLoader(
      async (keys) => {
        const users = await User.findAll({ where: { id: keys.map(key => key.id) } })

        function addAdditionalInfo (user, server) {
          if (user === undefined) {
            return null
          }

          return {
            id: user.id,
            name: user.name,
            discriminator: user.discriminator,
            color: userRoles.load({ server, id: user.id }).then(roles => roles.map(role => role.color)[0] || null),
            nickname: userNicknames.load({ server, id: user.id }).then(nicks => nicks.map(nick => nick.name)[0] || null)
          }
        }

        return keys.map(({ server, id }) => addAdditionalInfo(users.find(s => s.id === id), server))
      },
      {
        cacheKeyFn: ({ server, id }) => `${server}-${id}`
      }
    ),
    roles,
    messageVersions: new DataLoader(async (ids) => {
      const versions = await MessageVersion.findAll({
        where: {
          message: ids
        },
        order: [['timestamp', 'DESC']]
      })

      return ids.map(id => versions.filter(v => v.message === id))
    }),
    embeds: new DataLoader(async (messages) => {
      const embeds = await Promise.all(
        (await Embed.findAll({ where: { message: messages } })).map(embed => ({
          id: embed.id,
          message: embed.message,
          type: embed.type,
          title: embed.title,
          description: embed.description,
          url: embed.url,
          color: embed.color === null ? null : mapColor(embed.color),
          timestamp: embed.timestamp,
          footer: embedEntry(embed, 'footer', ['text', 'iconUrl', 'iconProxyUrl']),
          image: embedEntry(embed, 'image', ['url', 'proxyUrl', 'width', 'height']),
          thumbnail: embedEntry(embed, 'thumbnail', ['url', 'proxyUrl', 'width', 'height']),
          video: embedEntry(embed, 'video', ['url', 'proxyUrl', 'width', 'height']),
          provider: embedEntry(embed, 'provider', ['name', 'url']),
          author: embedEntry(embed, 'author', ['name', 'url', 'iconUrl', 'iconProxyUrl']),
          fields: () => embedFields.load(embed.id)
        }))
      )

      return messages.map(id => embeds.filter(e => e.message === id))
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
    if (channel === null) {
      return channel
    }
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
              deletedAt: null
            }
        )
      },
      limit,
      order: [['createdAt', 'ASC']]
    })

    return Promise.all(messages.map(async (msg) => {
      const versions = await request.loaders.messageVersions.load(msg.id)

      return {
        id: msg.id,
        author: request.loaders.users.load({ server: msg.server, id: msg.user }),
        server: msg.server,
        versions: permissions.has('manageMessages') ? versions : [versions[0]],
        createdAt: msg.createdAt,
        editedAt: versions.length > 1 ? versions[0].timestamp : null,
        deletedAt: msg.deletedAt,
        hasEmbeds: async () => (await Embed.findOne({
          attributes: [[Sequelize.fn('COUNT', Sequelize.col('id')), 'count']],
          where: { message: msg.id }
        })).get('count') > 0
      }
    }))
  },
  async embeds ({ message: messageId }, request) {
    const message = await Message.findOne({ where: { id: messageId } })
    if (message === undefined) {
      return []
    }

    const channel = await request.loaders.channels.load(message.channel)
    const permissions = (await getPermissions(request.user, channel.server)).channels[channel.id]

    if (permissions === undefined || !permissions.has('readMessages')) {
      return []
    }

    return request.loaders.embeds.load(messageId)
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
