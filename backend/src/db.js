const Sequelize = require('sequelize');
const db = new Sequelize('postgres://harmony:h4rm0ny@localhost/harmony');

module.exports = {
  Server: db.define('server', {
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
  }),
  Channel: db.define('channels', {
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
  }, {
    timestamps: false,
  }),
};
