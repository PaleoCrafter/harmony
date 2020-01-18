import { Extension, Node } from 'tiptap'
import { keymap } from 'prosemirror-keymap'

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

export class Submit extends Extension {
  get plugins () {
    return [
      keymap({
        Enter: this.options.onSubmit
      })
    ]
  }
}
