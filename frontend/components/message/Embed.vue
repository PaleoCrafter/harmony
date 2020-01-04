<template>
  <div :class="['embed', `embed--${embed.type.toLowerCase()}`]" :style="{ '--embed-accent': accentColor }">
    <Markdown
      v-if="embed.provider !== null"
      :tag="embed.provider.url !== null ? 'a' : 'div'"
      :attributes="embed.provider.url !== null ? { href: embed.provider.url, target: '_blank' } : {}"
      :content="embed.provider.name || ''"
      class="embed__provider"
    >
      {{ embed.provider.name === null ? embed.provider.url : '' }}
    </Markdown>
    <div v-if="embed.author !== null" class="embed__author">
      <img v-if="authorIconUrl !== null" :src="authorIconUrl" alt="author icon" class="embed__author-icon">
      <Markdown
        :tag="embed.author.url !== null ? 'a' : 'span'"
        :attributes="embed.author.url !== null ? { href: embed.author.url, target: '_blank' } : {}"
        :content="embed.author.name || ''"
      >
        {{ embed.author.name === null ? embed.author.url : '' }}
      </Markdown>
    </div>
    <Markdown
      v-if="embed.title !== null || embed.url !== null"
      :tag="embed.url !== null ? 'a' : 'div'"
      :attributes="embed.url !== null ? { href: embed.url, target: '_blank' } : {}"
      :content="embed.title || ''"
      class="embed__title"
    >
      {{ embed.title === null ? embed.url : '' }}
    </Markdown>
    <Markdown v-if="embed.description !== null" :content="embed.description" class="embed__description" embed />
    <div v-if="embed.fields.length > 0" class="embed__fields">
      <div
        v-for="(field, index) in groupedFields"
        :key="index"
        :class="['embed__field', { 'embed__field--inline': field.inline }]"
        :style="{ gridColumn: `${field.column}/span ${field.span}`, gridRow: `${field.row}` }"
      >
        <Markdown :content="field.name" class="embed__field-name" embed />
        <Markdown :content="field.value" class="embed__field-value" embed />
      </div>
    </div>
    <img v-if="embed.image !== null" :src="embed.image.proxyUrl" alt="image" class="embed__image">
    <img v-if="embed.thumbnail !== null" :src="embed.thumbnail.proxyUrl" alt="thumbnail" class="embed__thumbnail">
    <div v-if="embed.footer !== null || embed.timestamp !== null" class="embed__footer">
      <img v-if="footerIconUrl !== null" :src="footerIconUrl" alt="footer icon" class="embed__footer-icon">
      <div class="embed__footer-text">
        <Markdown v-if="embed.footer !== null" :content="embed.footer.text" tag="span" embed />
        <span v-if="embed.footer && embed.footer.text !== null && timestamp !== null" class="embed__footer-separator">•</span>
        <span v-if="timestamp !== null">{{ timestamp }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import Markdown from '@/components/message/Markdown.vue'

const gcd = (a, b) => a ? gcd(b % a, a) : b

const lcm = (a, b) => a * b / gcd(a, b)

export default {
  name: 'Embed',
  components: { Markdown },
  props: {
    embed: {
      type: Object,
      required: true
    }
  },
  computed: {
    accentColor () {
      const { r, g, b } = this.embed.color ?? { r: 32, g: 34, b: 37 }
      return `rgb(${r}, ${g}, ${b})`
    },
    authorIconUrl () {
      return this.embed.author?.proxyIconUrl ?? this.embed.author?.iconUrl ?? null
    },
    title () {
      return this.embed.title ?? this.embed.url
    },
    groupedFields () {
      const groups = this.embed.fields.reduce(
        ({ lastInline, result }, field) => {
          if (lastInline === undefined) {
            return { lastInline: field.inline, result: [...result, ...(field.inline ? [[field]] : [field])] }
          } else if (!lastInline && field.inline) {
            return { lastInline: true, result: [...result, [field]] }
          } else if (lastInline && field.inline) {
            const last = result.pop()
            return { lastInline: true, result: [...result, [...last, field]] }
          } else {
            return { lastInline: false, result: [...result, field] }
          }
        },
        { result: [] }
      ).result
      const requiredColumns = groups.filter(g => Array.isArray(g)).map(g => g.length).reduce(lcm)
      return groups.reduce(
        (acc, group, row) => {
          if (!Array.isArray(group)) {
            return [...acc, { ...group, column: 1, span: requiredColumns, row: row + 1 }]
          }
          const groupSpan = requiredColumns / group.length
          const inlineFields = group.map((f, index) => ({
            ...f,
            column: index * groupSpan + 1,
            span: groupSpan,
            row: row + 1
          }))
          return [...acc, ...inlineFields]
        },
        []
      )
    },
    footerIconUrl () {
      return this.embed.footer?.proxyIconUrl ?? this.embed.footer?.iconUrl ?? null
    },
    timestamp () {
      if (this.embed.timestamp === null) {
        return null
      }

      const format = new Intl.DateTimeFormat(undefined, {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
      })
      return format.format(Date.parse(this.embed.timestamp))
    }
  }
}
</script>

<style lang="scss">
.embed {
  display: inline-grid;
  white-space: normal;
  border-radius: 4px;
  padding: 0.5rem 1rem 1rem;
  background: #2f3136;
  border-left: var(--embed-accent) 4px solid;
  max-width: 520px;

  &__author {
    display: flex;
    align-items: center;
    font-size: 0.875rem;
    margin-top: 0.5rem;

    &-icon {
      max-width: 1.7rem;
      max-height: 1.7rem;
      margin-right: 0.5rem;
      border-radius: 100%;
    }

    .markdown {
      white-space: normal;
      color: white !important;
    }
  }

  &__provider {
    margin-top: 0.5rem;
    font-size: 0.75rem;
    color: #b9bbbe !important;
    white-space: normal;

    grid-column: 1;
    grid-row: 1;
  }

  &__title {
    font-weight: bold;
    margin-top: 0.5rem;
    grid-column: 1;
    grid-row: 2;
    white-space: normal;
  }

  &__description {
    margin-top: 0.5rem;
    font-size: 0.9rem;
    line-height: 1.175rem;
    white-space: pre-line;
    grid-column: 1;
    grid-row: 3;
  }

  &__fields {
    margin-top: 0.5rem;
    display: grid;
    grid-gap: 0.5rem;
  }

  &__field {
    &-name, &-value {
      font-size: 0.875rem;
      line-height: 1.125rem;
    }

    &-name {
      color: #72767d;
      font-weight: 600;
      margin-bottom: 2px;
    }
  }

  &__thumbnail, &__image {
    max-width: 400px;
    max-height: 300px;
    margin-top: 1rem;
    border-radius: 4px;
    grid-column: 1;
    grid-row: 5;
  }

  &__footer {
    display: flex;
    align-items: center;
    margin-top: 0.5rem;
    font-size: 0.75rem;
    line-height: 1rem;
    color: #72767d;
    grid-column: 1;
    grid-row: 6;

    &-icon {
      max-width: 1.4rem;
      max-height: 1.4rem;
      margin-right: 0.5rem;
      border-radius: 100%;
    }

    .markdown {
      white-space: normal;
    }

    &-separator {
      margin: 0 0.25rem;
      font-weight: bold;
      color: rgba(255, 255, 255, 0.06);
    }
  }

  .markdown__code, .code-block code {
    background: #202225 !important;
  }

  .code-block, blockquote {
    max-width: 100%;
  }

  &--rich {
    .embed__thumbnail {
      max-width: 80px;
      max-height: 80px;
      grid-column: 2;
      grid-row: 1/span 6;
      margin-top: 0.5rem;
      margin-left: 1rem;
    }
  }

  &--image, &--unknown {
    grid-auto-columns: minmax(auto, min-content);
  }
}
</style>
