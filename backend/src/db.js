const Sequelize = require('sequelize');
const db = new Sequelize('postgres://harmony:h4rm0ny@localhost/harmony');

const Server = db.define('server', {
  id: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true,
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false,
  },
  iconUrl: {
    type: Sequelize.STRING,
  },
  active: {
    type: Sequelize.BOOLEAN,
    allowNull: false,
  },
}, {
  timestamps: false,
});

const Role = db.define('role', {
  id: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true,
  },
  server: {
    type: Sequelize.BIGINT,
    allowNull: false,
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false,
  },
  permissions: {
    type: Sequelize.BIGINT,
    allowNull: false,
  },
  deletedAt: {
    type: Sequelize.DATE,
  },
}, {
  timestamps: false,
});

const Channel = db.define('channel', {
  id: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true,
  },
  server: {
    type: Sequelize.BIGINT,
    allowNull: false,
  },
  name: {
    type: Sequelize.STRING,
    allowNull: false,
  },
  type: {
    type: Sequelize.ENUM('TEXT', 'NEWS'),
    allowNull: false,
  },
  deletedAt: {
    type: Sequelize.DATE,
  },
}, {
  timestamps: false,
});

const PermissionOverride = db.define('permissionOverride', {
  channel: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true,
  },
  type: {
    type: Sequelize.ENUM('UNKNOWN', 'ROLE', 'USER'),
    allowNull: false,
    primaryKey: true,
  },
  target: {
    type: Sequelize.BIGINT,
    allowNull: false,
    primaryKey: true,
  },
  allowed: {
    type: Sequelize.BIGINT,
    allowNull: false,
  },
  denied: {
    type: Sequelize.BIGINT,
    allowNull: false,
  },
}, {
  timestamps: false,
});

Channel.hasMany(PermissionOverride, { foreignKey: 'channel', foreignKeyConstraint: false, constraints: false });

module.exports = {
  Server,
  Role,
  Channel,
  PermissionOverride,
};
