const path = require('path')
const fs = require('fs')
const { ApolloServer, gql } = require('apollo-server-express')
const DataLoader = require('dataloader')
const Sequelize = require('sequelize')
const { Op, QueryTypes } = Sequelize
const { database, Server, Channel, User, Message, MessageVersion, Role, Embed, EmbedField, Attachment, Reaction } = require('./db')
const { checkAuth, getPermissions } = require('./auth')
const search = require('./search')

const typeDefs = gql(fs.readFileSync(path.join(__dirname, 'schema.gqls'), 'utf8'))

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

const ATTACHMENT_TYPES = {
  jpg: 'IMAGE',
  jpeg: 'IMAGE',
  webp: 'IMAGE',
  gif: 'IMAGE',
  png: 'IMAGE',
  mp4: 'VIDEO',
  webm: 'VIDEO',
  mp3: 'AUDIO',
  wav: 'AUDIO',
  ogg: 'AUDIO'
}

function mergeEmbeds (embeds) {
  function merge (target, source) {
    if (source.image) {
      target.images.push(source.image)
    }
  }

  return embeds.reduce(
    (acc, embed) => {
      const existing = embed.url === null ? null : acc.find(e => e.url === embed.url)
      if (existing) {
        merge(existing, embed)
        return acc
      } else {
        return [...acc, { ...embed, image: undefined, images: embed.image !== null ? [embed.image] : [] }]
      }
    },
    []
  )
}

function prepareMessage (message, versions, editedAt, permissions, request) {
  const infoKey = { message: message.id, canSeeDeleted: permissions.has('manageMessages') }

  return {
    id: message.idSuffix ? `${message.id}-${message.idSuffix}` : message.id,
    ref: message.id,
    author: request.loaders.users.load({ server: message.server, id: message.user }),
    server: message.server,
    versions: permissions.has('manageMessages') || message.user === request.user.id ? versions : [versions[0]],
    createdAt: message.createdAt,
    editedAt,
    deletedAt: message.deletedAt,
    hasReactions: async () => (await request.loaders.messageCounts.load(infoKey)).reactions > 0,
    hasEmbeds: async () => (await request.loaders.messageCounts.load(infoKey)).embeds > 0,
    hasAttachments: async () => (await request.loaders.messageCounts.load(infoKey)).attachments > 0
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

      return (await channelLoader.loadMany(ids)).filter(channel => channel !== null)
        .sort(
          (a, b) => a.categoryPosition === b.categoryPosition
            ? a.position - b.position
            : a.categoryPosition - b.categoryPosition
        )
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
      const variables = await Promise.all(
        keys.map(async ({ server, id }, index) => ({
          query: `($server${index}::bigint, $user${index}::bigint, $canSeeDeleted${index}::boolean)`,
          values: {
            [`server${index}`]: server,
            [`user${index}`]: id,
            [`canSeeDeleted${index}`]: (await getPermissions(user, server)).server.has('manageRoles')
          }
        }))
      )
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
        JOIN (VALUES ${variables.map(v => v.query).join(', ')}) AS conditions (s, u, canSeeDeleted)
          ON "userroles"."server" = s AND "userroles"."user" = u
        WHERE canSeeDeleted OR "roles"."deletedAt" IS NULL
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
            bot: user.bot,
            color: () => userRoles.load({ server, id: user.id })
              .then(roles => roles.filter(({ color: { r, g, b } }) => r !== 0 || b !== 0 || g !== 0).map(role => role.color)[0] || null),
            nickname: () => userNicknames.load({ server, id: user.id }).then(nicks => nicks.map(nick => nick.name)[0] || null)
          }
        }

        return keys.map(({ server, id }) => addAdditionalInfo(users.find(s => s.id === id), server))
      },
      {
        cacheKeyFn: ({ server, id }) => `${server}-${id}`
      }
    ),
    roles,
    userRoles,
    userNicknames,
    messages: new DataLoader(async (ids) => {
      const messages = await Message.findAll({ where: { id: ids } })

      return ids.map(id => messages.find(msg => msg.id === id) || null)
    }),
    messageCounts: new DataLoader(async (keys) => {
      const messages = keys.map(k => k.message)
      const embedCounts = await Embed.findAll({
        attributes: ['message', [Sequelize.fn('COUNT', Sequelize.col('*')), 'count']],
        where: { message: messages },
        group: ['message']
      })
      const attachmentCounts = await Attachment.findAll({
        attributes: ['message', [Sequelize.fn('COUNT', Sequelize.col('*')), 'count']],
        where: { message: messages },
        group: ['message']
      })

      const reactionVariables = keys.map(({ message, canSeeDeleted }, index) => ({
        query: `($message${index}::bigint, $canSeeDeleted${index}::boolean)`,
        values: {
          [`message${index}`]: message,
          [`canSeeDeleted${index}`]: canSeeDeleted
        }
      }))
      const reactionCounts = await database.query(
        `
        SELECT
          "messagereactions"."message" AS "message",
          COUNT(*) AS "count"
        FROM "messagereactions"
        JOIN (VALUES ${reactionVariables.map(v => v.query).join(', ')}) AS conditions (msg, canSeeDeleted)
          ON "messagereactions"."message" = msg AND (canSeeDeleted OR "messagereactions"."deletedAt" IS NULL)
        GROUP BY "message"
        `,
        {
          bind: reactionVariables.reduce((acc, v) => ({ ...acc, ...v.values }), {}),
          type: QueryTypes.SELECT
        }
      )

      return messages.map(id => ({
        embeds: parseInt((embedCounts.find(e => e.message === id) || { get: () => '0' }).get('count')),
        attachments: parseInt((attachmentCounts.find(a => a.message === id) || { get: () => '0' }).get('count')),
        reactions: parseInt((reactionCounts.find(r => r.message === id) || { count: '0' }).count)
      }))
    }),
    messageVersions: new DataLoader(async (ids) => {
      const versions = await MessageVersion.findAll({
        where: {
          message: ids.filter(id => id !== 'ignored')
        },
        order: [['timestamp', 'DESC']]
      })

      return ids.map(id => versions.filter(v => v.message === id))
    }),
    reactions: new DataLoader(async (keys) => {
      const messages = keys.map(k => k.message)
      const reactions = await database.query(
        `
        SELECT
          "messagereactions"."message" AS "message",
          "messagereactions"."type" AS "type",
          "messagereactions"."emoji" AS "emoji",
          CASE WHEN "messagereactions"."type" = 'UNICODE' THEN NULL ELSE "messagereactions"."emojiId" END AS "emojiId",
          bool_or("messagereactions"."emojiAnimated") AS "emojiAnimated",
          MIN("messagereactions"."createdAt") AS "createdAt",
          COUNT("counter"."user") AS "count"
        FROM "messagereactions"
        LEFT JOIN "messagereactions" AS "counter"
          ON "counter"."message" = "messagereactions"."message"
          AND "counter"."user" = "messagereactions"."user"
          AND "counter"."type" = "messagereactions"."type"
          AND "counter"."emoji" = "messagereactions"."emoji"
          AND "counter"."emojiId" = "messagereactions"."emojiId"
          AND "counter"."deletedAt" IS NULL
        WHERE "messagereactions".message IN (${messages.map((_, i) => `$${i + 1}::bigint`).join(', ')})
        GROUP BY "messagereactions"."message", "messagereactions"."type", "messagereactions"."emoji", "messagereactions"."emojiId"
        ORDER BY "createdAt" ASC
        `,
        {
          bind: messages,
          type: QueryTypes.SELECT
        }
      )

      return keys.map(({ message, canSeeDeleted }) => {
        const messageReactions = reactions.filter(r => r.message === message)
        return messageReactions
          .filter(r => canSeeDeleted || r.count > 0)
          .map(reaction => ({
            type: reaction.type,
            emoji: reaction.emoji,
            emojiId: reaction.emojiId,
            emojiAnimated: reaction.emojiAnimated,
            count: reaction.count
          }))
      })
    }, {
      cacheKeyFn: ({ message, canSeeDeleted }) => `${message}-${canSeeDeleted}`
    }),
    embeds: new DataLoader(async (keys) => {
      const messages = keys.map(k => k.message)
      const embeds = await Promise.all(
        (await Embed.findAll({ where: { message: messages }, order: [['id', 'ASC']] })).map(embed => ({
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

      return messages.map(id => mergeEmbeds(embeds.filter(e => e.message === id)))
    }),
    attachments: new DataLoader(async (keys) => {
      const messages = keys.map(k => k.message)
      const attachments = await Promise.all(
        (await Attachment.findAll({ where: { message: messages } })).map(attachment => ({
          message: attachment.message,
          type: ATTACHMENT_TYPES[attachment.name.split('.').pop()] || 'FILE',
          name: attachment.name,
          url: attachment.url,
          proxyUrl: attachment.proxyUrl,
          width: attachment.width,
          height: attachment.height,
          spoiler: attachment.spoiler
        }))
      )

      return messages.map(id => attachments.filter(e => e.message === id))
    }),
    searchMessages: new DataLoader(async (ids) => {
      const messages = await Message.findAll({ where: { id: ids.filter(id => id !== 'ignored') } })

      return ids.map(id => messages.find(msg => msg.id === id) || null)
    })
  }
}

const queryResolver = {
  identity (parent, args, { request }) {
    const { id, username, discriminator, timezone } = request.user
    return { user: { id, name: username, discriminator }, timezone }
  },
  async servers (parent, args, { request }) {
    return (await request.loaders.servers.loadMany(request.user.servers.map(server => server.id))).filter(s => s !== null)
  },
  server (parent, { id: requestedId }, { request }) {
    return request.user.servers.find(s => s.id === requestedId) ? request.loaders.servers.load(requestedId) : null
  },
  async channel (parent, { id: requestedId }, { request }) {
    const channel = await request.loaders.channels.load(requestedId)
    if (channel === null) {
      return channel
    }
    const permissions = (await getPermissions(request.user, channel.server)).channels[channel.id]
    return permissions && permissions.has('readMessages') ? channel : null
  },
  user (parent, { server, id }, { request }) {
    return request.user.servers.find(s => s.id === server) ? request.loaders.users.load({ server, id }) : null
  },
  role (parent, { server, id }, { request }) {
    return request.user.servers.find(s => s.id === server) ? request.loaders.roles.load({ server, id }) : null
  },
  async messages (parent, { channel: channelId, paginationMode, paginationReference, limit }, { request }) {
    if (limit > 100) {
      throw new Error('You may request at most 100 messages at once!')
    }

    const channel = await request.loaders.channels.load(channelId)
    const permissions = (await getPermissions(request.user, channel.server)).channels[channel.id]

    if (permissions === undefined || !permissions.has('readMessages')) {
      return []
    }

    const { message: refMessageId, minTime, maxTime } = paginationReference

    const refMessage = refMessageId ? await Message.findOne({ where: { id: refMessageId } }) : null
    if (refMessageId && !refMessage) {
      return []
    }

    let paginationCondition = {}
    if (refMessage && paginationMode !== 'AROUND') {
      paginationCondition = {
        [Op.or]: [
          { createdAt: { [paginationMode === 'BEFORE' ? Op.lt : Op.gt]: refMessage.createdAt } },
          {
            [Op.and]: {
              createdAt: refMessage.createdAt,
              id: { [paginationMode === 'BEFORE' ? Op.lt : Op.gt]: refMessageId }
            }
          }
        ]
      }
    } else if (refMessage) {
      const firstMessage = (await Message.findAll({
        where: {
          [Op.or]: [
            { id: refMessageId },
            { createdAt: { [Op.lt]: refMessage.createdAt } },
            {
              [Op.and]: {
                createdAt: refMessage.createdAt,
                id: { [Op.lt]: refMessageId }
              }
            }
          ]
        },
        limit: Math.ceil(limit / 2),
        order: [['createdAt', 'DESC'], ['id', 'DESC']]
      })).pop()

      paginationCondition = {
        [Op.or]: [
          { id: firstMessage.id },
          { createdAt: { [Op.gt]: firstMessage.createdAt } },
          {
            [Op.and]: {
              createdAt: firstMessage.createdAt,
              id: { [Op.gt]: refMessageId }
            }
          }
        ]
      }
    }

    const messages = await Message.findAll({
      where: {
        channel: channel.id,
        createdAt: {
          [Op.and]: { [Op.gt]: minTime, [Op.lt]: maxTime }
        },
        ...paginationCondition,
        ...(
          permissions.has('manageMessages')
            ? {}
            : {
              [Op.or]: {
                deletedAt: null,
                user: request.user.id
              }
            }
        )
      },
      limit,
      order: [['createdAt', paginationMode === 'BEFORE' ? 'DESC' : 'ASC'], ['id', paginationMode === 'BEFORE' ? 'DESC' : 'ASC']]
    })

    return Promise.all(messages.map(async (msg) => {
      const versions = await request.loaders.messageVersions.load(msg.id)

      return prepareMessage(msg, versions, versions.length > 1 ? versions[0].timestamp : null, permissions, request)
    }))
  },
  async messageDetails (parent, { message: messageId }, { request }) {
    const message = await request.loaders.messages.load(messageId)
    if (message === null) {
      return null
    }

    const channel = await request.loaders.channels.load(message.channel)
    const permissions = (await getPermissions(request.user, channel.server)).channels[channel.id]

    if (permissions === undefined || !permissions.has('readMessages')) {
      return null
    }

    const key = { message: messageId, canSeeDeleted: permissions.has('manageMessages') }

    return {
      embeds: () => request.loaders.embeds.load(key),
      attachments: () => request.loaders.attachments.load(key),
      reactions: () => request.loaders.reactions.load(key)
    }
  },
  async reactors (parent, { message: messageId, type, emoji, emojiId }, { request }) {
    const message = await request.loaders.messages.load(messageId)
    if (message === null) {
      return []
    }

    const channel = await request.loaders.channels.load(message.channel)
    const permissions = (await getPermissions(request.user, channel.server)).channels[channel.id]

    if (permissions === undefined || !permissions.has('readMessages')) {
      return []
    }

    const reactions = await Reaction.findAll({
      where: {
        message: messageId,
        type,
        emoji,
        emojiId: emojiId || '0',
        ...(
          permissions.has('manageMessages')
            ? {}
            : {
              deletedAt: null
            }
        )
      },
      order: [['createdAt', 'ASC'], ['deletedAt', 'ASC', 'NULLS FIRST']]
    })

    return reactions.map(r => ({
      user: () => request.loaders.users.load({ server: message.server, id: r.user }),
      createdAt: r.createdAt,
      deletedAt: r.deletedAt
    }))
  },
  userDetails (parent, { server, id }, { request }) {
    return request.user.servers.find(s => s.id === server)
      ? {
        roles: request.loaders.userRoles.load({ server, id }),
        nicknames: request.loaders.userNicknames.load({ server, id })
      }
      : null
  },
  async search (parent, { server, parameters }, { request }) {
    if (!request.user.servers.find(s => s.id === server)) {
      return { total: 0, entries: [] }
    }

    function prepareVersion (messageId, version) {
      return {
        timestamp: version.timestamp,
        content: version.content
      }
    }

    const permissions = (await getPermissions(request.user, server)).channels
    const readableChannels = Object.keys(permissions).filter(c => permissions[c].has('readMessages'))
    const manageableChannels = Object.keys(permissions).filter(c => permissions[c].has('manageMessages'))

    let total, totalPages, rawMessages
    try {
      ({ total, totalPages, messages: rawMessages } = await search(
        server,
        request.user.id,
        parameters.query,
        parameters.sort,
        readableChannels,
        manageableChannels,
        parameters.page
      ))
    } catch (e) {
      return { total: 0, totalPages: 1, entries: [], error: 'Invalid search parameters' }
    }

    if (total === 0 || rawMessages.length === 0) {
      return { total: 0, totalPages: 1, entries: [] }
    }

    const messageIds = rawMessages.map(msg => msg.id)

    const contextMessages = await database.query(
      `
        SELECT
          (SELECT "prevMessage"."id" FROM "messages" AS "prevMessage"
           WHERE "prevMessage"."channel" = "messages"."channel" AND
             ("prevMessage"."createdAt" < "messages"."createdAt"
                OR ("prevMessage"."createdAt" = "messages"."createdAt" AND "prevMessage"."id" < "messages"."id"))
           ORDER BY "prevMessage"."createdAt" DESC, "prevMessage"."id" DESC LIMIT 1) AS "prev",
          "messages"."id" AS "message",
          (SELECT "nextMessage"."id" FROM "messages" AS "nextMessage"
           WHERE "nextMessage"."channel" = "messages"."channel" AND
             ("nextMessage"."createdAt" > "messages"."createdAt"
                OR ("nextMessage"."createdAt" = "messages"."createdAt" AND "nextMessage"."id" > "messages"."id"))
           ORDER BY "nextMessage"."createdAt" ASC, "nextMessage"."id" ASC LIMIT 1) AS "next"
        FROM "messages"
        WHERE "messages"."id" IN (${messageIds.map((_, i) => `$${i + 1}::bigint`).join(', ')})
        `,
      {
        bind: messageIds,
        type: QueryTypes.SELECT
      }
    )

    const orderedContextMessages = messageIds.map(msgId => contextMessages.find(c => c.message === msgId)).filter(c => c !== undefined)
    const entries = Promise.all(orderedContextMessages.map(async ({ prev: prevId, message: msgId, next: nextId }) => {
      const searchResult = rawMessages.find(msg => msg.id === msgId).content
      const [prev, message, next] = await request.loaders.searchMessages.loadMany([prevId || 'ignored', msgId, nextId || 'ignored'])
      const channelPermissions = permissions[message.channel]
      const [prevVersions, versions, nextVersions] = await request.loaders.messageVersions.loadMany([prevId || 'ignored', msgId, nextId || 'ignored'])

      const messageResult = { channel: request.loaders.channels.load(message.channel), previous: null, message: null, next: null }

      message.idSuffix = 'search-result'
      versions[0].content = searchResult
      messageResult.message = prepareMessage(
        message,
        [prepareVersion(msgId, versions[0])],
        versions.length > 1 ? versions[0].timestamp : null,
        channelPermissions,
        request
      )

      if (prev && (prev.deletedAt === null || channelPermissions.has('manageMessages') || prev.user === request.user.id)) {
        prev.idSuffix = 'search-result'
        messageResult.previous = prepareMessage(
          prev,
          [prepareVersion(prevId, prevVersions[0])],
          prevVersions.length > 1 ? prevVersions[0].timestamp : null,
          channelPermissions,
          request
        )
      }

      if (next && (next.deletedAt === null || channelPermissions.has('manageMessages') || next.user === request.user.id)) {
        next.idSuffix = 'search-result'
        messageResult.next = prepareMessage(
          next,
          [prepareVersion(nextId, nextVersions[0])],
          nextVersions.length > 1 ? nextVersions[0].timestamp : null,
          channelPermissions,
          request
        )
      }

      return messageResult
    }))

    return { total, totalPages, entries }
  },
  searchSuggestions (parent, { server }, { request }) {
    if (!request.user.servers.find(s => s.id === server)) {
      return { users: [] }
    }

    return {
      users ({ query }) {
        return database.query(
          `
          SELECT
            "id", "name", "discriminator", "nickname"
          FROM (
            SELECT
              "users"."id" AS "id",
              "users"."name" AS "name",
              "users"."discriminator" AS "discriminator",
              (
                SELECT "usernicknames"."name"
                FROM "usernicknames"
                WHERE "usernicknames"."server" = $server AND "usernicknames"."user" = "users"."id"
                ORDER BY "usernicknames"."timestamp" DESC
                LIMIT 1
              ) AS "nickname",
              (
                SELECT COUNT(*)
                FROM "messages"
                WHERE "messages"."server" = $server AND "messages"."user" = "users"."id"
              ) AS "messageCount"
            FROM "users"
          ) AS "users"
          WHERE
            "nickname" ILIKE $query OR CONCAT("name", '#', "discriminator") ILIKE $query
                AND "discriminator" != 'HOOK'
                AND "messageCount" > 0
          LIMIT 10
          `,
          {
            bind: { server, query: `%${query}%` },
            type: QueryTypes.SELECT
          }
        )
      }
    }
  },
  async redirectMessage (parent, { id }, { request }) {
    const message = await Message.findOne({ where: { id } })

    if (!message || !request.user.servers.find(s => s.id === message.server)) {
      return null
    }

    const permissions = (await getPermissions(request.user, message.server)).channels[message.channel]

    if (permissions === undefined || !permissions.has('readMessages')) {
      return null
    }

    return message
  },
  async latestMessage (parent, { channel: channelId, before }, { request }) {
    const channel = await request.loaders.channels.load(channelId)
    const permissions = (await getPermissions(request.user, channel.server)).channels[channel.id]

    if (permissions === undefined || !permissions.has('readMessages')) {
      return []
    }

    const message = await Message.findOne({
      where: {
        channel: channelId,
        ...(
          permissions.has('manageMessages')
            ? {}
            : {
              [Op.or]: {
                deletedAt: null,
                user: request.user.id
              }
            }
        ),
        ...(
          before === null
            ? {}
            : {
              createdAt: {
                [Op.lt]: before
              }
            }
        )
      },
      order: [['createdAt', 'DESC']]
    })

    if (!message || !request.user.servers.find(s => s.id === message.server)) {
      return null
    }

    return { id: message.id, createdAt: message.createdAt }
  }
}

module.exports = {
  init (app) {
    app.use((req, res, next) => {
      req.loaders = initLoaders(req.user)
      next()
    })
    const server = new ApolloServer({
      typeDefs,
      resolvers: { Query: queryResolver },
      formatError: (err) => {
        // eslint-disable-next-line no-console
        console.error(err.extensions.exception)
        return err
      },
      context: context => ({ request: context.req }),
      playground: {
        settings: {
          'request.credentials': 'include'
        }
      }
    })
    app.use('/api/graphql', checkAuth, server.getMiddleware({ path: '/' }))
  }
}
