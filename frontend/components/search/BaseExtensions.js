import { Node } from 'tiptap'

export class Doc extends Node {
  get name () {
    return 'doc'
  }

  get schema () {
    return {
      content: 'root'
    }
  }
}

export class Root extends Node {
  get name () {
    return 'root'
  }

  get schema () {
    return {
      content: 'inline*',
      group: 'block',
      draggable: false,
      parseDOM: [{
        tag: 'div'
      }],
      toDOM: () => ['div', { class: 'search-box__root' }, 0]
    }
  }
}
