const { Client } = require('@elastic/elasticsearch')

const INDEX = 'messages'
const client = new Client({ node: process.env.ELASTIC_HOST })

module.exports = async function search (server, query, sortOrder, readableChannels, manageableChannels, after) {
  const highlightTag = `__HARMONY_SEARCH_${Date.now()}__`

  const filter = {
    bool: {
      minimum_should_match: 1,
      must: [
        { term: { server } },
        { terms: { 'channel.id': readableChannels } }
      ],
      should: [
        { term: { deletedAt: 0 } },
        { terms: { 'channel.id': manageableChannels } }
      ]
    }
  }

  const { body: rawResult } = await client.search({
    index: INDEX,
    body: {
      size: 10,
      query: {
        bool: {
          must: {
            query_string: {
              query,
              default_field: 'content.cs.lowercase',
              default_operator: 'AND',
              quote_field_suffix: '.exact'
            }
          },
          filter
        }
      },
      search_after: after
        ? [sortOrder ? after.timestamp : undefined, after.id].filter(a => a !== undefined)
        : undefined,
      sort: {
        ...(sortOrder ? { timestamp: sortOrder === 'ASCENDING' ? 'asc' : 'desc' } : {}),
        tie_breaker_id: 'asc'
      },
      highlight: {
        fields: {
          'content.cs': {
            matched_fields: ['content.cs', 'content.cs.lowercase', 'content.cs.exact', 'content.cs.lowercase.exact'],
            number_of_fragments: 0,
            pre_tags: [highlightTag],
            post_tags: [highlightTag],
            type: 'fvh'
          }
        }
      }
    }
  })

  return {
    total: rawResult.hits.total.value,
    messages: rawResult.hits.hits.map(msg => ({ id: msg._id, content: msg.highlight ? msg.highlight['content.cs'][0] : msg._source.content }))
  }
}
