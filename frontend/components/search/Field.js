import { Node } from 'tiptap'
import { nodeInputRule } from 'tiptap-commands'
import { keymap } from 'prosemirror-keymap'
import { chainCommands, deleteSelection, joinBackward, selectNodeBackward } from 'prosemirror-commands'
import { NodeSelection } from 'prosemirror-state'

const deleteCommand = chainCommands(deleteSelection, joinBackward, selectNodeBackward)

function undoInputRuleNoop (state, dispatch, view) {
  const plugins = state.plugins
  for (let i = 0; i < plugins.length; i++) {
    const plugin = plugins[i]
    if (plugin.spec.isInputRules && plugin.getState(state)) {
      if (dispatch) {
        dispatch(state.tr.setSelection(NodeSelection.create(state.doc, state.selection.$cursor.before(0))))
      }
      deleteCommand(state, dispatch, view)
      return true
    }
  }
  return false
}

export default class Field extends Node {
  get name () {
    return 'field'
  }

  get schema () {
    return {
      attrs: {
        field: {}
      },
      group: 'inline',
      inline: true,
      selectable: false,
      toDOM: node => [
        'span',
        {
          class: 'search-box__field'
        },
        `${node.attrs.field}:`
      ]
    }
  }

  inputRules ({ type }) {
    return [
      nodeInputRule(/([A-Za-z]+):/, type, match => ({ field: match[1] }))
    ]
  }

  get plugins () {
    return [
      keymap({
        Backspace: undoInputRuleNoop
      })
    ]
  }
}
