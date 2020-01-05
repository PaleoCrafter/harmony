import { BatchHttpLink } from 'apollo-link-batch-http'

export default function ({ req }) {
  const uri = process.client ? '/api/graphql' : 'http://backend:3000/api/graphql'

  const linkOptions = { uri }
  if (process.server && req && req.headers && req.headers.cookie) {
    linkOptions.headers = { cookie: req.headers.cookie }
  }

  return {
    tokenName: 'connect.sid',
    link: new BatchHttpLink(linkOptions),
    defaultHttpLink: false
  }
}
