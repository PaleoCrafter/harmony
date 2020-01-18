import { Node } from 'tiptap'

export default class FieldValue extends Node {
  get name () {
    return 'fieldValue'
  }

  get schema () {
    return {
      attrs: {
        id: {},
        label: {}
      },
      group: 'inline',
      inline: true,
      selectable: false,
      toDOM: node => [
        'span',
        {
          class: `search-box__field-value`
        },
        node.attrs.label
      ]
    }
  }
}
