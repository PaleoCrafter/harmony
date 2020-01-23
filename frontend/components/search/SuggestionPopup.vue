<script>
import { TextSelection } from 'prosemirror-state'
import { findField } from '@/components/search/Field.js'

function calculateInitialIndex (count, direction) {
  return (count + (direction >= 1 ? direction - 1 : direction)) % count
}

export default {
  name: 'SuggestionPopup',
  props: {
    groups: {
      type: Array,
      required: true
    },
    editor: {
      type: Object,
      default: () => null
    },
    defaultSuggestion: {
      type: Number,
      default: () => null
    }
  },
  data () {
    return {
      selectedItem: null
    }
  },
  computed: {
    context () {
      return {
        insertValue: (id, label) => {
          const state = this.editor.state
          const { queryStart, queryEnd } = findField(state)
          this.editor.view.dispatch(
            state.tr
              .setSelection(TextSelection.create(state.doc, queryStart, queryEnd))
              .replaceSelectionWith(state.schema.node('fieldValue', { id, label }))
              .insertText(' ')
              .scrollIntoView()
          )
        },
        insertNode: (type, attrs) => {
          const state = this.editor.state
          let transaction = state.tr
          if (this.editor?.state?.doc?.firstChild?.childCount === 0) {
            transaction = transaction.setSelection(TextSelection.create(state.doc, 1, 1))
          }
          this.editor.view.dispatch(
            transaction.replaceSelectionWith(state.schema.node(type, attrs))
              .scrollIntoView()
          )
        }
      }
    }
  },
  watch: {
    groups () {
      this.selectedItem = null
      if (this.defaultSuggestion !== null) {
        this.selectItem(0, this.defaultSuggestion)
      }
    }
  },
  methods: {
    performSelectionAction () {
      if (this.selectedItem === null) {
        return
      }

      const group = this.groups[this.selectedItem.group]
      group.action(group.items[this.selectedItem.item], this.context)
    },
    selectItem (groupIndex, itemIndex) {
      if (groupIndex < this.groups.length && itemIndex < this.groups[groupIndex].items.length) {
        this.selectedItem = { group: groupIndex, item: itemIndex }
      }
    },
    isItemSelected (groupIndex, itemIndex) {
      return this.selectedItem !== null && this.selectedItem.group === groupIndex && this.selectedItem.item === itemIndex
    },
    moveSelection (direction) {
      if (this.selectedItem === null) {
        const groupCount = this.groups.length
        const newGroup = calculateInitialIndex(groupCount, direction)
        const itemCount = this.groups[newGroup].items.length
        const newItem = calculateInitialIndex(itemCount, direction)
        this.selectItem(newGroup, newItem)
        return
      }

      const { group, item } = this.selectedItem

      let newGroup = group
      let newItem = item + direction

      if (newItem < 0 || newItem >= this.groups[group].items.length) {
        const groupCount = this.groups.length
        newGroup = ((group + groupCount) + direction) % groupCount
        const itemCount = this.groups[newGroup].items.length
        newItem = calculateInitialIndex(itemCount, direction)
      }

      this.selectItem(newGroup, newItem)
    },
    onKeyDown (event) {
      // pressing up arrow
      if (event.keyCode === 38) {
        this.moveSelection(-1)
        return true
      }
      // pressing down arrow
      if (event.keyCode === 40) {
        this.moveSelection(1)
        return true
      }

      // pressing enter, tab or space
      if (event.keyCode === 13 || event.keyCode === 9 || event.keyCode === 32) {
        this.performSelectionAction()
        return this.selectedItem !== null || event.keyCode === 9
      }

      return false
    }
  },
  render (h) {
    const groupElements = this.groups.map(({ title, items, render }, groupIndex) =>
      h(
        'li',
        { class: 'suggestion-popup__group' },
        [
          h('span', { class: 'suggestion-popup__group-title' }, [title]),
          h(
            'ul',
            { class: 'suggestion-popup__items' },
            items.length > 0
              ? items.map((item, itemIndex) =>
                h(
                  'li',
                  {
                    class: [
                      'suggestion-popup__item',
                      { 'suggestion-popup__item--selected': this.isItemSelected(groupIndex, itemIndex) }
                    ],
                    on: {
                      mouseover: () => this.selectItem(groupIndex, itemIndex),
                      click: this.performSelectionAction
                    }
                  },
                  render(h, item)
                )
              )
              : [h('li', { class: 'suggestion-popup__item' }, 'Could not find any matching suggestions')]
          )
        ]
      )
    )

    return h('ul', { class: 'suggestion-popup' }, groupElements)
  }
}
</script>

<style lang="scss">
.suggestion-popup {
  font-family: 'Open Sans', sans-serif;
  list-style-type: none;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin: 0;
  padding: 0;
  line-height: 1.2;

  &__group {
    display: flex;
    flex-direction: column;
    align-items: stretch;

    &-title {
      font-weight: 300;
      text-transform: uppercase;
      font-size: 0.8rem;
      padding: 0.5rem 0.6rem;
    }
  }

  &__items {
    list-style-type: none;
    margin: 0;
    padding: 0;
  }

  &__item {
    padding: 0.5rem 0.6rem;
    cursor: pointer;
    overflow: hidden;
    white-space: nowrap;
    position: relative;

    &:after {
      z-index: 1;
      content: "";
      position: absolute;
      top: 0;
      right: 0;
      height: 100%;
      width: 30px;
      background: linear-gradient(90deg, rgba(54, 57, 63, 0), #36393f 80%);
    }

    &--selected {
      background: #23262a;
      border-radius: 0.25rem;

      &:after {
        background: linear-gradient(90deg, rgba(35, 38, 42, 0), #23262a 80%);
      }
    }
  }
}
</style>
