export default [
  {
    title: 'Search Options',
    items: [
      { field: 'from', example: 'user' },
      { field: 'mentions', example: 'user' },
      { field: 'has', example: 'linked, embed or file' },
      { field: 'in', example: 'channel' }
    ],
    action ({ field }, { insertNode }) {
      insertNode('field', { field })
    },
    render (h, { field, example }) {
      return [
        h('span', { class: 'search-suggestions__options-field' }, [`${field}:`]),
        h('span', { class: 'search-suggestions__options-example' }, [example])
      ]
    }
  }
]
