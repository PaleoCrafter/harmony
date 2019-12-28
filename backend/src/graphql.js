const graphqlHTTP = require('express-graphql');
const DataLoader = require('dataloader');
const { Server, Channel } = require('./db');
const { checkAuth } = require('./auth');
const { buildSchema } = require('graphql');

const schema = buildSchema(`
  type Query {
    identity: User!
  
    servers: [Server!]!
    server(id: ID!): Server
  }
  
  type Server {
    id: ID!
    name: String!
    iconUrl: String
    channels: [Channel!]
  }
  
  type Channel {
    id: ID!
    name: String!
  }
  
  type User {
    id: ID!
    name: String!
    discriminator: String!
  }
`);

function initLoaders(user) {
  const channelLoader = new DataLoader(async ids => {
    const servers = await Channel.findAll({ where: { id: ids } });
    return ids.map(id => servers.find(s => s.id === id) || null);
  });

  function loadServerChannels(server) {
    return () => {
      const ids = user.getPermissions(server.id);
    };
  }

  return {
    servers: new DataLoader(async ids => {
      const servers = await Server.findAll({ where: { active: true, id: ids } });
      return ids.map(id => servers.find(s => s.id === id) || null);
    }),
    channels: channelLoader,
  };
}

const root = {
  async servers(args, request) {
    return (await request.loaders.servers.loadMany(request.user.servers.map(server => server.id))).filter(s => s !== null);
  },
  server({ id: requestedId }, request) {
    return request.loaders.servers.load(requestedId);
  },
};

module.exports = {
  init(app) {
    app.use((req, res, next) => {
      req.loaders = initLoaders(req.user);
      next();
    });
    app.use('/graphql', checkAuth, graphqlHTTP({
      schema: schema,
      rootValue: root,
      graphiql: true,
    }));
  },
};
