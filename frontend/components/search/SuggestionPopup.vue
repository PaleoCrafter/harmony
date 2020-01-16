<script>
function calculateInitialIndex (count, direction) {
  return (count + (direction >= 1 ? direction - 1 : direction)) % count
}

export default {
  name: 'SuggestionPopup',
  props: {
    groups: {
      type: Array,
      required: true
    }
  },
  data () {
    return {
      selectedItem: null
    }
  },
  watch: {
    groups () {
      this.selectedItem = null
    }
  },
  methods: {
    selectItem (groupIndex, itemIndex) {
      this.selectedItem = { group: groupIndex, item: itemIndex }
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

      // pressing enter or tab
      if (event.keyCode === 13 || event.keyCode === 9) {
        this.$emit('suggestion')
        return true
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
            items.map((item, itemIndex) =>
              h(
                'li',
                {
                  class: [
                    'suggestion-popup__item',
                    { 'suggestion-popup__item--selected': this.isItemSelected(groupIndex, itemIndex) }
                  ],
                  on: {
                    mouseover: () => this.selectItem(groupIndex, itemIndex)
                  }
                },
                render(h, item)
              )
            )
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

    &--selected {
      background: #23262a;
      border-radius: 0.25rem;
    }
  }
}
</style>
