<template>
  <div :class="['search-box', { 'search-box--focus': popupActive }]">
    <EditorContent :editor="editor" class="search-box__container" />
    <SearchIcon class="search-box__icon" size="1x" stroke-width="3" />
    <div v-if="popupActive" class="search-box__popup">
      <SuggestionPopup ref="suggestions" :groups="activeSuggestions" />
    </div>
  </div>
</template>

<script>
import { Editor, EditorContent, Text } from 'tiptap'
import { Placeholder } from 'tiptap-extensions'
import { SearchIcon } from 'vue-feather-icons'
import { Doc, Root } from '@/components/search/BaseExtensions'
import Field from '@/components/search/Field'
import ParenthesisMatching from '@/components/search/ParenthesisMatching'
import defaultSuggestions from '@/components/search/suggestions/default'
import SuggestionPopup from '@/components/search/SuggestionPopup.vue'

export default {
  name: 'SearchBox',
  components: { SuggestionPopup, EditorContent, SearchIcon },
  data () {
    return {
      editor: null,
      popupActive: false,
      activeSuggestions: defaultSuggestions
    }
  },
  mounted () {
    const self = this
    this.editor = new Editor({
      content: '',
      editorProps: {
        attributes: {
          class: 'search-box__content',
          role: 'textbox',
          spellcheck: false
        },
        handleKeyDown (view, event) {
          if (event.keyCode === 27) {
            self.editor.blur()
            return true
          }

          return self.$refs.suggestions.onKeyDown(event)
        }
      },
      emptyDocument: {
        type: 'doc',
        content: [{
          type: 'root'
        }]
      },
      extensions: [
        new Doc(),
        new Root(),
        new Text(),
        new Field(),
        new Placeholder({
          emptyNodeText: 'Search',
          emptyNodeClass: 'search-box__placeholder'
        }),
        new ParenthesisMatching()
      ],
      useBuiltInExtensions: false,
      onInit ({ state }) {
        console.log(state)
      },
      onFocus () {
        self.popupActive = true
      },
      onBlur () {
        self.popupActive = false
      }
    })
  },
  beforeDestroy () {
    this.editor.destroy()
  }
}
</script>

<style lang="scss">
.search-box {
  background: #202225;
  border-radius: 4px;
  cursor: text;
  overflow: visible;
  display: flex;
  align-items: center;
  height: 24px;
  width: 160px;
  padding: 2px 0.5rem 2px 2px;
  font-family: monospace;
  margin-left: auto;
  margin-right: 1rem;
  position: relative;
  font-size: 0.9rem;

  &--focus {
    width: 240px;
  }

  &__container {
    flex: 1;
    border: none;
    overflow: visible;
    height: 20px;
    line-height: 20px;
    vertical-align: baseline;
  }

  &__content {
    outline: none;
    user-select: text;
    white-space: pre-wrap;
    overflow-wrap: break-word;
    overflow-x: auto;
    overflow-y: hidden;
    -webkit-user-modify: read-write-plaintext-only;
    box-sizing: content-box;
    vertical-align: baseline;
    height: inherit;
    text-align: initial;
    padding: 0 2px;
  }

  &__root {
    display: inline-block;
    min-width: 1px;
    white-space: pre;
    vertical-align: baseline;
    overflow: visible;
    position: relative;
  }

  &__parens {
    font-weight: bold;
    color: yellow;

    &--unmatched {
      color: red;
    }
  }

  &__placeholder:before {
    content: attr(data-empty-text);
    pointer-events: none;
    color: #72767d;
    float: left;
    height: 0;
  }

  &__icon {
    color: #72767d;
  }

  &__field {
    background-color: #34383d;
    border-radius: 2px;
    display: inline-block;
    padding: 0 2px;
    margin-left: -2px;
  }

  &__popup {
    position: absolute;
    top: 100%;
    margin-top: 0.5rem;
    background: #36393F;
    border: #202225;
    border-radius: 0.25rem;
    width: 280px;
    left: 50%;
    margin-left: -140px;
    padding: 0.6rem;
    cursor: default;
    box-shadow: 0 0 0 1px rgba(32, 34, 37, .6), 0 2px 10px 0 rgba(0, 0, 0, .2);
  }
}

@import '~/components/search/suggestions/default';
</style>
