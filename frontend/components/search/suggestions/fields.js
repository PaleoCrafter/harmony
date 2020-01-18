import usersQuery from '@/apollo/queries/user-suggestions.gql'
import channelsQuery from '@/apollo/queries/server-channels.gql'

const users = (title) => {
  return async (query, { server, apollo }) => {
    const { data: { searchSuggestions: { users } } } = await apollo.query({
      query: usersQuery,
      variables: {
        server,
        query
      }
    })

    return [
      {
        title,
        items: users,
        action ({ id, name, discriminator }, { insertValue }) {
          insertValue(id, `${name}#${discriminator}`)
        },
        render (h, { name, discriminator, nickname }) {
          const displayName = nickname ?? name
          return [
            h('span', { class: 'search-suggestions__user-name' }, [displayName]),
            ' ',
            h('span', { class: 'search-suggestions__user-exact-name' }, [`${name}#${discriminator}`])
          ]
        }
      }
    ]
  }
}

export default {
  has: query => [
    {
      title: 'Message contains',
      items: ['link', 'embed', 'file', 'video', 'image', 'sound'].filter(item => item.includes(query)),
      action (value, { insertValue }) {
        insertValue(value, value)
      },
      render (h, item) {
        return [
          h('strong', [item])
        ]
      }
    }
  ],
  from: users('From User'),
  mentions: users('Mentions User'),
  in: async (query, { server, apollo }) => {
    const { data: { server: { channels } } } = await apollo.query({
      query: channelsQuery,
      variables: {
        id: server
      }
    })

    const adjustedQuery = query.startsWith('#') ? query.slice(1) : query

    return [
      {
        title: 'In Channel',
        items: channels.filter(channel => channel.name.includes(adjustedQuery)),
        action ({ id, name }, { insertValue }) {
          insertValue(id, name)
        },
        render (h, channel) {
          return [
            h('strong', [channel.name])
          ]
        }
      }
    ]
  }
}
