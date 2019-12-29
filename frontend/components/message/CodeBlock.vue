<template>
  <pre v-hljs class="code-block"><code :class="language"><slot /></code></pre>
</template>

<script>
export default {
  name: 'CodeBlock',
  props: {
    language: {
      type: String,
      default: () => undefined
    }
  }
}
</script>

<style lang="scss">
.code-block {
  box-sizing: border-box;
  max-width: 90%;
  font-family: Consolas, Andale Mono WT, Andale Mono, Lucida Console, Lucida Sans Typewriter, DejaVu Sans Mono, Bitstream Vera Sans Mono, Liberation Mono, Nimbus Mono L, Monaco, Courier New, Courier, monospace;
  font-size: 0.75rem;
  line-height: 1rem;
  margin-top: 6px;
  white-space: pre-wrap;
  background-clip: border-box;
  border-radius: 4px;
  padding: 0;

  code {
    display: block;
    font-size: 1rem;
    line-height: 1.125rem;
    text-indent: 0;
    white-space: pre-wrap;
    scrollbar-width: thin;
    scrollbar-color: #202225 #2f3136;
    background: #2f3136;
    border: 1px solid #202225;
    overflow-x: auto;
    padding: .5em;
    border-radius: 4px;
    text-size-adjust: none;
    color: #abb2bf;
  }

  .hljs-comment, .hljs-quote {
    color: #5c6370;
    font-style: italic
  }

  .hljs-doctag, .hljs-keyword, .hljs-formula {
    color: #c678dd
  }

  .hljs-section, .hljs-name, .hljs-selector-tag, .hljs-deletion, .hljs-subst {
    color: #e06c75
  }

  .hljs-literal {
    color: #56b6c2
  }

  .hljs-string, .hljs-regexp, .hljs-addition, .hljs-attribute, .hljs-meta-string {
    color: #98c379
  }

  .hljs-built_in, .hljs-class .hljs-title {
    color: #e6c07b
  }

  .hljs-attr, .hljs-variable, .hljs-template-variable, .hljs-type, .hljs-selector-class, .hljs-selector-attr, .hljs-selector-pseudo, .hljs-number {
    color: #d19a66
  }

  .hljs-symbol, .hljs-bullet, .hljs-link, .hljs-meta, .hljs-selector-id, .hljs-title {
    color: #61aeee
  }

  .hljs-emphasis {
    font-style: italic
  }

  .hljs-strong {
    font-weight: bold
  }

  .hljs-link {
    text-decoration: underline
  }
}
</style>
