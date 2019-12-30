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
  MessageVersion
}
