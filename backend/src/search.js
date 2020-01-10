const { Client } = require('@elastic/elasticsearch')

const INDEX = 'messages'
const client = new Client({ node: process.env.ELASTIC_HOST })

export default function search (query, sortOrder, after) {
  const highlightTag = `__HARMONY_SEARCH_${Date.now()}__`

  return client.search({
    index: INDEX,
    body: {
      size: 10,
      query: {
        query_string: {
          query,
          default_field: 'content.cs.lowercase',
          default_operator: 'AND',
          quote_field_suffix: '.exact'
        }
      },
      search_after: after
        ? [sortOrder ? after.timestamp : undefined, after.id].filter(a => a !== undefined)
        : undefined,
      sort: {
        timestamp: sortOrder,
        tie_breaker_id: 'asc'
      },
      highlight: {
        fields: {
          'content.cs': {
            match_fields: ['content.cs', 'content.cs.lowercase', 'content.cs.exact', 'content.cs.lowercase.exact'],
            number_of_fragments: 0,
            pre_tags: [highlightTag],
            post_tags: [highlightTag],
            type: 'fvh'
          }
        }
      }
    }
  })
}
