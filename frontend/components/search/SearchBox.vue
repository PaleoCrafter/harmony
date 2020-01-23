<template>
  <div @mousedown.capture="acquireFocus" :class="['search-box', { 'search-box--focus': popupActive, 'search-box--empty': editorEmpty }]">
    <EditorContent ref="editor" :editor="editor" class="search-box__container" />
    <SearchIcon v-if="editorEmpty" class="search-box__icon" size="1x" stroke-width="3" />
    <XIcon v-else @click="clear" class="search-box__icon search-box__icon--clear" size="1x" stroke-width="3" />
    <div v-if="popupActive" class="search-box__popup">
      <SuggestionPopup ref="suggestions" :groups="activeSuggestions" :editor="editor" :default-suggestion="defaultSuggestion" />
    </div>
  </div>
</template>

<script>
import { Editor, EditorContent, Text } from 'tiptap'
import { SearchIcon, XIcon } from 'vue-feather-icons'
import { Doc, Root, Submit } from '@/components/search/BaseExtensions'
import Field from '@/components/search/Field'
import ParenthesisMatching from '@/components/search/ParenthesisMatching'
import defaultSuggestions from '@/components/search/suggestions/default'
import fieldSuggestions from '@/components/search/suggestions/fields'
import SuggestionPopup from '@/components/search/SuggestionPopup.vue'
import serialize from '@/components/search/serialize'
import FieldValue from '@/components/search/FieldValue'
import Operators from '@/components/search/Operators'
import Quotes from '@/components/search/Quotes'

export default {
  name: 'SearchBox',
  components: { SuggestionPopup, EditorContent, SearchIcon, XIcon },
  data () {
    return {
      editor: null,
      popupActive: false,
      activeSuggestions: defaultSuggestions,
      defaultSuggestion: null
    }
  },
  computed: {
    editorEmpty () {
      return this.editor?.state?.doc?.firstChild?.childCount === 0
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
        },
        clipboardTextSerializer: serialize
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
        new Field({
          onQuery: (field, query) => {
            const suggestionResolver = fieldSuggestions[field]

            if (!suggestionResolver) {
              this.resetSuggestions()
              return
            }

            Promise.resolve(fieldSuggestions[field](
              query,
              {
                server: this.$route.params.id,
                apollo: this.$apollo.provider.defaultClient
              }))
              .then((suggestions) => {
                this.activeSuggestions = suggestions
                this.defaultSuggestion = 0
              })
          },
          onCommit: this.resetSuggestions,
          reset: this.resetSuggestions
        }),
        new FieldValue(),
        new ParenthesisMatching(),
        new Operators(),
        new Quotes(),
        new Submit({
          onSubmit: () => {
            this.submit()
            return true
          }
        })
      ],
      useBuiltInExtensions: false,
      onFocus () {
        self.popupActive = true
      },
      onBlur (event) {
        self.popupActive = false
      }
    })
  },
  beforeDestroy () {
    this.editor.destroy()
  },
  methods: {
    acquireFocus (event) {
      if (!this.$refs.editor.$el.contains(event.target)) {
        event.preventDefault()
      }
    },
    resetSuggestions () {
      this.activeSuggestions = defaultSuggestions
      this.defaultSuggestion = null
    },
    clear () {
      this.editor.clearContent()
    },
    submit () {
      if (this.$refs.suggestions.onKeyDown({ keyCode: 13 })) {
        return
      }

      if (this.editorEmpty) {
        this.$emit('submit', null)
      } else {
        this.$emit('submit', serialize(this.editor.state.doc, true))
      }
      this.editor.blur()
    }
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
  padding: 2px 0.5rem 2px 2px;
  font-family: monospace;
  margin-left: auto;
  margin-right: 1rem;
  position: relative;
  font-size: 0.9rem;

  &--empty {
    .search-box__content {
      width: 160px;

      &:before {
        content: 'Search';
        pointer-events: none;
        color: #72767d;
        float: left;
        height: 0;
      }
    }
  }

  &--focus {
    .search-box__content {
      width: 240px;
    }
  }

  &__container {
    flex: 1;
    border: none;
    overflow: hidden;
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
    padding: 0 2px 20px;
    width: 240px;
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
    color: #f0e354;

    &--unmatched {
      color: #f04747;
    }
  }

  &__operator {
    font-weight: bold;
    color: #adb2ba;
  }

  &__icon {
    color: #72767d;
    margin-left: 0.5rem;

    &--clear {
      color: #dcddde;
      cursor: pointer;

      &:hover {
        color: white;
      }
    }
  }

  &__field, &__field-value {
    background-color: #34383d;
    border-radius: 2px;
    display: inline-block;
    padding: 0 2px;
    margin-left: -2px;

    &--invalid {
      color: #f04747;
    }
  }

  &__popup {
    position: absolute;
    top: 100%;
    margin-top: 0.5rem;
    background: #36393F;
    border: #202225;
    border-radius: 0.25rem;
    width: 300px;
    left: 50%;
    transform: translateX(-50%);
    padding: 0.6rem;
    cursor: default;
    box-shadow: 0 0 0 1px rgba(32, 34, 37, .6), 0 2px 10px 0 rgba(0, 0, 0, .2);
  }
}

@import '~/components/search/suggestions/styles';
</style>
