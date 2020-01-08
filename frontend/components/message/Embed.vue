<template>
  <div
    :class="['embed', `embed--${embed.type.toLowerCase()}`, { 'embed--images': embed.images.length > 0 }]"
    :style="{ '--embed-accent': accentColor }"
  >
    <Markdown
      v-if="embed.provider !== null"
      :tag="embed.provider.url !== null ? 'a' : 'div'"
      :attributes="embed.provider.url !== null ? { href: embed.provider.url, target: '_blank', rel: 'noopener' } : {}"
      :content="embed.provider.name || ''"
      class="embed__provider embed__grid-item"
    >
      {{ embed.provider.name === null ? embed.provider.url : '' }}
    </Markdown>
    <div v-if="embed.author !== null" class="embed__author embed__grid-item">
      <img v-if="authorIconUrl !== null" :src="authorIconUrl" alt="author icon" class="embed__author-icon">
      <Markdown
        :tag="embed.author.url !== null ? 'a' : 'span'"
        :attributes="embed.author.url !== null ? { href: embed.author.url, target: '_blank', rel: 'noopener' } : {}"
        :content="embed.author.name || ''"
      >
        {{ embed.author.name === null ? embed.author.url : '' }}
      </Markdown>
    </div>
    <Markdown
      v-if="embed.title !== null"
      :tag="embed.url !== null ? 'a' : 'div'"
      :attributes="embed.url !== null ? { href: embed.url, target: '_blank', rel: 'noopener' } : {}"
      :content="embed.title"
      class="embed__title embed__grid-item"
    />
    <Markdown v-if="embed.description !== null" :content="embed.description" class="embed__description embed__grid-item" embed />
    <div v-if="embed.fields.length > 0" class="embed__fields embed__grid-item">
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
    <div
      v-if="embed.images.length > 0"
      :class="['embed__images', 'embed__grid-item', { 'embed__images--multiple': embed.images.length > 1 }]"
    >
      <div
        v-for="image in embed.images"
        :style="{ width: calculateImageStyle(image).width }"
        class="embed__image-container"
      >
        <LazyImage
          :src="getImageUrl(image)"
          :style="calculateImageStyle(image)"
          alt="image"
        />
      </div>
    </div>
    <div
      v-if="embed.thumbnail !== null && embed.video === null"
      :style="{ width: calculateImageStyle(embed.thumbnail).width }"
      class="embed__thumbnail embed__grid-item"
    >
      <LazyImage
        :src="thumbnailUrl"
        :style="calculateImageStyle(embed.thumbnail)"
        alt="thumbnail"
      />
    </div>
    <div
      v-else-if="embed.thumbnail !== null"
      :style="{ width: calculateImageStyle(embed.thumbnail).width }"
      class="embed__video embed__grid-item"
    >
      <div :style="calculateImageStyle(embed.thumbnail)" class="embed__video-container">
        <div class="embed__video-content">
          <iframe v-if="playingVideo" :src="videoUrl" width="100%" height="100%" allowfullscreen />
          <LazyImage
            v-else-if="embed.thumbnail"
            :src="thumbnailUrl"
            alt="thumbnail"
            class="embed__thumbnail"
          />
          <div v-if="!playingVideo" class="embed__video-actions">
            <a @click.prevent="playingVideo = true" :href="embed.video.url" target="_blank" rel="noopener" aria-label="Play video">
              <PlayIcon fill="currentColor" stroke="none" />
            </a>
            <a :href="embed.url" target="_blank" rel="noopener" aria-label="Open video">
              <ExternalLinkIcon />
            </a>
          </div>
        </div>
      </div>
    </div>
    <div v-if="embed.footer !== null || embed.timestamp !== null" class="embed__footer embed__grid-item">
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
import { ExternalLinkIcon, PlayIcon } from 'vue-feather-icons'
import Markdown from '@/components/message/Markdown.vue'
import LazyImage from '@/components/message/LazyImage.vue'

const gcd = (a, b) => a ? gcd(b % a, a) : b

const lcm = (a, b) => a * b / gcd(a, b)

export default {
  name: 'Embed',
  components: { PlayIcon, ExternalLinkIcon, LazyImage, Markdown },
  props: {
    embed: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      playingVideo: false
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
    thumbnailUrl () {
      return this.embed.thumbnail?.proxyUrl ?? this.embed.thumbnail?.url ?? null
    },
    videoUrl () {
      const url = this.embed.video?.url
      if (url === null) {
        return undefined
      }

      const query = url.includes('?') ? '&' : '?'

      return `${url}${query}autoplay=1`
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
  },
  methods: {
    calculateImageStyle (image) {
      if (image === null) {
        return undefined
      }

      const width = image.width ?? 400
      const height = image.height ?? 300
      const aspect = width / height
      const minAspect = 4 / 3
      const styles = { width: `${width}px`, paddingTop: `${100 / aspect}%` }

      if (width > 400 || height > 300) {
        styles.width = aspect > minAspect ? '400px' : `${aspect * 300}px`
      }

      return styles
    },
    getImageUrl (image) {
      return image?.proxyUrl ?? image?.url ?? null
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
  grid-template-columns: minmax(0, auto) minmax(0, auto);
  grid-auto-rows: auto;
  column-gap: 1rem;

  a {
    color: #00b0f4;
    text-decoration: none;
    unicode-bidi: bidi-override;
    direction: ltr;
    word-break: break-word;

    &:hover, &:focus {
      text-decoration: underline;
    }
  }

  @media (max-width: 768px) {
    display: grid;
    max-width: 100%;
    flex-basis: 0;
  }

  &__grid-item {
    min-width: 0;
    margin-top: 0.5rem;
  }

  &__provider {
    font-size: 0.75rem;
    color: #b9bbbe !important;
    white-space: normal;
    grid-column: 1;
    grid-row: 1;
  }

  &__author {
    display: flex;
    align-items: center;
    font-size: 0.875rem;
    grid-column: 1;
    grid-row: 2;

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

  &__title {
    font-weight: bold;
    grid-column: 1;
    grid-row: 3;
    white-space: nowrap;
  }

  &__description {
    font-size: 0.9rem;
    line-height: 1.175rem;
    white-space: pre-line;
    grid-column: 1;
    grid-row: 4;
  }

  &__fields {
    display: grid;
    grid-gap: 0.5rem;
    grid-column: 1;
    grid-row: 5;
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

  &__thumbnail, &__images {
    max-width: 400px;
    max-height: 300px;
    margin-top: 1rem;
    border-radius: 4px;
    grid-column: 1;
    grid-row: 6;
    overflow: hidden;

    @media (max-width: 768px) {
      max-width: 100%;
    }
  }

  &__image {
    max-width: 400px;
    max-height: 300px;

    @media (max-width: 768px) {
      max-width: 100%;
    }
  }

  &__images {
    display: grid;
    grid-template-columns: 50% 50%;
    grid-auto-rows: 1fr;
    grid-gap: 0.25rem;
    max-height: unset;

    &--multiple {
      max-width: 300px;

      @media (max-width: 768px) {
        max-width: 100%;
      }
    }

    .embed__image-container {
      max-width: 100%;

      &:only-child {
        grid-column: 1/span 2
      }
    }
  }

  &__video {
    position: relative;
    display: flex;
    align-items: stretch;
    justify-content: stretch;
    margin-top: 0.5rem;
    grid-column: 1;
    grid-row: 6;
    max-width: 400px;
    max-height: 300px;
    border-radius: 4px;
    overflow: hidden;

    @media (max-width: 768px) {
      max-width: 100%;
    }

    &-container {
      position: relative;
      display: flex;
      align-items: stretch;
      justify-content: stretch;
      flex: 1;
      flex-basis: 0;
      max-width: 100%;
    }

    &-content {
      position: absolute;
      top: 0;
      bottom: 0;
      left: 0;
      right: 0;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    iframe {
      border: none;
      border-radius: 4px;
    }

    .lazy-image {
      width: 100%;
      height: 100%;
    }

    &-actions {
      position: absolute;
      display: grid;
      align-items: center;
      justify-content: center;
      grid-template-columns: 1fr 1fr;
      grid-gap: 0.5rem;
      background: rgba(0, 0, 0, 0.5);
      padding: 1rem;
      border-radius: 2rem;
    }

    a {
      display: flex;
      color: rgba(255, 255, 255, 0.6) !important;
      transition: color 0.2s ease-in-out;

      &:hover, &:focus, &:active {
        color: white !important;
      }
    }
  }

  &__footer {
    display: flex;
    align-items: center;
    font-size: 0.75rem;
    line-height: 1rem;
    color: #72767d;
    grid-column: 1;
    grid-row: 7;

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
      grid-row: 1/span 7;
      margin-top: 0.5rem;

      @media (max-width: 768px) {
        max-width: 60px;
        max-height: 60px;
      }
    }
  }

  &--unknown, &--image, &--video, &--images {
    grid-template-columns: minmax(0, min-content);
  }
}
</style>
