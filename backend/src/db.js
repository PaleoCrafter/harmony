const Sequelize = require('sequelize')
const db = new Sequelize(process.env.DB_URL)

const Server = db.define('server', {
  id: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false
  },
  iconUrl: {
    type: Sequelize.STRING
  },
  active: {
    type: Sequelize.BOOLEAN,
    allowNull: false
  }
}, {
  timestamps: false
})

const Role = db.define('role', {
  id: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  server: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false
  },
  color: {
    type: Sequelize.STRING,
    allowNull: false
  },
  position: {
    type: Sequelize.INTEGER,
    allowNull: false
  },
  permissions: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  deletedAt: {
    type: Sequelize.DATE
  }
}, {
  timestamps: false
})

const Channel = db.define('channel', {
  id: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  server: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false
  },
  category: {
    type: Sequelize.STRING,
    allowNull: true
  },
  categoryPosition: {
    type: Sequelize.INTEGER,
    allowNull: false
  },
  position: {
    type: Sequelize.INTEGER,
    allowNull: false
  },
  type: {
    type: Sequelize.ENUM('TEXT', 'NEWS'),
    allowNull: false
  },
  deletedAt: {
    type: Sequelize.DATE
  }
}, {
  timestamps: false
})

const PermissionOverride = db.define('permissionoverride', {
  channel: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  type: {
    type: Sequelize.ENUM('UNKNOWN', 'ROLE', 'USER'),
    allowNull: false,
    primaryKey: true
  },
  target: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  allowed: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  denied: {
    type: Sequelize.BIGINT,
    allowNull: false
  }
}, {
  timestamps: false
})

Channel.hasMany(PermissionOverride, { as: 'PermissionOverride', foreignKey: 'channel', foreignKeyConstraint: false, constraints: false })

const User = db.define('user', {
  id: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false
  },
  discriminator: {
    type: Sequelize.STRING,
    allowNull: false
  },
  bot: {
    type: Sequelize.BOOLEAN,
    allowNull: false
  }
}, {
  timestamps: false
})

const UserNickname = db.define('usernicknames', {
  server: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  user: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  timestamp: {
    type: Sequelize.DATE,
    allowNull: false,
    primaryKey: true
  },
  name: {
    type: Sequelize.STRING
  }
}, {
  timestamps: false
})

const Message = db.define('message', {
  id: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  server: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  channel: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  user: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  webhookName: {
    type: Sequelize.STRING,
    allowNull: true
  },
  createdAt: {
    type: Sequelize.DATE
  },
  deletedAt: {
    type: Sequelize.DATE
  }
}, {
  timestamps: false
})

const MessageVersion = db.define('messageversion', {
  message: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  timestamp: {
    type: Sequelize.DATE,
    allowNull: false,
    primaryKey: true
  },
  content: {
    type: Sequelize.STRING,
    allowNull: false
  }
}, {
  timestamps: false
})

const Embed = db.define('messageembed', {
  id: {
    type: Sequelize.INTEGER,
    allowNull: false,
    primaryKey: true
  },
  message: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  type: {
    type: Sequelize.ENUM('UNKNOWN', 'IMAGE', 'LINK', 'RICH', 'VIDEO'),
    allowNull: false
  },
  title: {
    type: Sequelize.STRING
  },
  description: {
    type: Sequelize.STRING
  },
  url: {
    type: Sequelize.STRING
  },
  color: {
    type: Sequelize.STRING
  },
  timestamp: {
    type: Sequelize.DATE
  },

  footerText: {
    type: Sequelize.STRING
  },
  footerIconUrl: {
    type: Sequelize.STRING
  },
  footerIconProxyUrl: {
    type: Sequelize.STRING
  },

  imageUrl: {
    type: Sequelize.STRING
  },
  imageProxyUrl: {
    type: Sequelize.STRING
  },
  imageWidth: {
    type: Sequelize.INTEGER
  },
  imageHeight: {
    type: Sequelize.INTEGER
  },

  thumbnailUrl: {
    type: Sequelize.STRING
  },
  thumbnailProxyUrl: {
    type: Sequelize.STRING
  },
  thumbnailWidth: {
    type: Sequelize.INTEGER
  },
  thumbnailHeight: {
    type: Sequelize.INTEGER
  },

  videoUrl: {
    type: Sequelize.STRING
  },
  videoProxyUrl: {
    type: Sequelize.STRING
  },
  videoWidth: {
    type: Sequelize.INTEGER
  },
  videoHeight: {
    type: Sequelize.INTEGER
  },

  providerName: {
    type: Sequelize.STRING
  },
  providerUrl: {
    type: Sequelize.STRING
  },

  authorName: {
    type: Sequelize.STRING
  },
  authorUrl: {
    type: Sequelize.STRING
  },
  authorIconUrl: {
    type: Sequelize.STRING
  },
  authorIconProxyUrl: {
    type: Sequelize.STRING
  }
}, {
  timestamps: false
})

const EmbedField = db.define('messageembedfield', {
  embed: {
    type: Sequelize.INTEGER,
    allowNull: false,
    primaryKey: true
  },
  position: {
    type: Sequelize.INTEGER,
    allowNull: false,
    primaryKey: true
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false
  },
  value: {
    type: Sequelize.STRING,
    allowNull: false
  },
  inline: {
    type: Sequelize.BOOLEAN,
    allowNull: false
  }
}, {
  timestamps: false
})

const Attachment = db.define('messageattachment', {
  message: {
    type: Sequelize.BIGINT,
    allowNull: false
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false
  },
  url: {
    type: Sequelize.STRING,
    allowNull: false
  },
  proxyUrl: {
    type: Sequelize.STRING,
    allowNull: false
  },
  width: {
    type: Sequelize.INTEGER
  },
  height: {
    type: Sequelize.INTEGER
  },
  spoiler: {
    type: Sequelize.BOOLEAN,
    allowNull: false
  }
}, {
  timestamps: false
})
Attachment.removeAttribute('id')

const Reaction = db.define('messagereaction', {
  message: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  user: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true
  },
  type: {
    type: Sequelize.ENUM('UNICODE', 'CUSTOM'),
    allowNull: false,
    primaryKey: true
  },
  emoji: {
    type: Sequelize.STRING,
    allowNull: false,
    primaryKey: true
  },
  emojiId: {
    type: Sequelize.STRING,
    allowNull: false,
    primaryKey: true
  },
  emojiAnimated: {
    type: Sequelize.BOOLEAN,
    allowNull: false
  },
  createdAt: {
    type: Sequelize.DATE,
    allowNull: false
  },
  deletedAt: {
    type: Sequelize.DATE
  }
}, {
  timestamps: false
})

Message.hasMany(MessageVersion, { as: 'versions', foreignKey: 'message', foreignKeyConstraint: false, constraints: false })

module.exports = {
  database: db,
  Server,
  Role,
  Channel,
  PermissionOverride,
  User,
  UserNickname,
  Message,
  MessageVersion,
  Embed,
  EmbedField,
  Attachment,
  Reaction
}
