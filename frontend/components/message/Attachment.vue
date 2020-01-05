<template>
  <div :class="['attachment', `attachment--${attachment.type.toLowerCase()}`, { 'attachment--spoiler': attachment.spoiler && !spoilerRevealed }]">
    <div class="attachment__content">
      <template v-if="attachment.type === 'FILE' || attachment.type === 'AUDIO'">
        <FileIcon v-if="attachment.type === 'FILE'" class="attachment__icon" size="2.5x" />
        <MusicIcon v-else class="attachment__icon" size="2.5x" />
        <a :href="attachment.url" target="_blank" rel="noopener" class="attachment__name">{{ attachment.name }}</a>
        <a :href="attachment.url" target="_blank" rel="noopener" aria-label="Download" class="attachment__button attachment__download">
          <DownloadIcon size="1.5x" />
        </a>
      </template>
      <audio v-if="attachment.type === 'AUDIO'" :src="attachment.proxyUrl" controls />
      <div
        v-if="attachment.type === 'VIDEO' || attachment.type === 'IMAGE'"
        :style="{ width: mediaStyle.width }"
        :class="['attachment__media', { 'attachment__media--active': playingVideo }]"
      >
        <div :style="mediaStyle" class="attachment__media-container">
          <div class="attachment__media-content">
            <div class="attachment__media-header">
              <span class="attachment__name">{{ attachment.name }}</span>
              <a
                :href="attachment.url"
                target="_blank"
                rel="noopener"
                aria-label="Download"
                class="attachment__button attachment__download"
              >
                <DownloadIcon size="1.5x" />
              </a>
            </div>
            <template v-if="attachment.type === 'VIDEO'">
              <video
                ref="video"
                :src="attachment.proxyUrl"
                :poster="posterUrl"
                :controls="playingVideo"
                width="100%"
                height="100%"
                preload="metadata"
                playsinline
              />
              <button v-if="!playingVideo" @click.prevent="playVideo" aria-label="Play video" class="attachment__button attachment__play">
                <PlayIcon fill="currentColor" stroke="none" />
              </button>
            </template>
            <LazyImage v-else :src="attachment.proxyUrl" :alt="attachment.name" />
          </div>
        </div>
      </div>
    </div>
    <div v-if="attachment.spoiler && !spoilerRevealed" @click.capture.stop="spoilerRevealed = true" class="attachment__spoiler-warning">
      <span>Spoiler</span>
    </div>
  </div>
</template>

<script>
import { DownloadIcon, FileIcon, PlayIcon, MusicIcon } from 'vue-feather-icons'
import LazyImage from '@/components/message/LazyImage.vue'

export default {
  name: 'Attachment',
  components: { LazyImage, FileIcon, DownloadIcon, PlayIcon, MusicIcon },
  props: {
    attachment: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      playingVideo: false,
      spoilerRevealed: false
    }
  },
  computed: {
    mediaProps () {
      const width = this.attachment.width ?? 400
      const height = this.attachment.height ?? 300
      const aspect = width / height
      const minAspect = 4 / 3
      const props = { width, height, aspect }

      if (width > 400 || height > 300) {
        props.width = aspect > minAspect ? 400 : aspect * 300
        props.height = Math.round(aspect > minAspect ? 400 / aspect : 300)
      }

      return props
    },
    posterUrl () {
      return `${this.attachment.proxyUrl}?format=jpeg&width=${this.mediaProps.width}&height=${this.mediaProps.height}`
    },
    mediaStyle () {
      return { width: `${this.mediaProps.width}px`, paddingTop: `${100 / this.mediaProps.aspect}%` }
    }
  },
  methods: {
    playVideo () {
      this.$refs.video.play()
      this.playingVideo = true
    }
  }
}
</script>

<style lang="scss">
.attachment {
  position: relative;
  max-width: 520px;
  border-radius: 4px;
  overflow: hidden;

  @media (max-width: 768px) {
    max-width: 100%;
  }

  &__content {
    display: grid;
    align-items: center;
    grid-template-columns: auto minmax(0, auto) 1fr;
    grid-auto-rows: auto;
    max-width: 100%;
  }

  &--spoiler {
    pointer-events: none !important;

    .attachment__content {
      filter: blur(50px);
    }

    .attachment__spoiler-warning {
      position: absolute;
      top: 0;
      left: 0;
      bottom: 0;
      right: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      pointer-events: auto;

      span {
        text-transform: uppercase;
        padding: 0.5rem 0.75rem;
        border-radius: 2rem;
        font-weight: bold;
        background: rgba(0, 0, 0, 0.6);
      }

      &:hover span {
        background: rgba(0, 0, 0, 0.9);
      }
    }
  }

  &--file, &--audio {
    border: 1px solid rgba(47, 49, 54, .6);
    background: rgba(47, 49, 54, .3);
    padding: 0.75rem;
  }

  &--video, &--image {
    align-self: flex-start;

    .attachment__content {
      grid-template-columns: minmax(0, auto);
    }
  }

  &__icon {
    margin-right: 0.5rem;
  }

  &__name {
    flex: 1;
    flex-basis: 0;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
    margin-right: 0.5rem;
  }

  &__button {
    display: flex;
    align-items: center;
    justify-content: center;
    color: rgba(255, 255, 255, 0.6) !important;
    transition: color 0.2s ease-in-out;
    border: none;
    background: none;
    cursor: pointer;

    &:hover, &:focus, &:active {
      color: white !important;
    }
  }

  &__download {
    margin-left: auto;
  }

  audio {
    grid-column: 1/span 3;
    grid-row: 2;
    width: 100%;
    margin-top: 0.5rem;
  }

  &__play {
    position: absolute;
    background: rgba(0, 0, 0, 0.5);
    padding: 0.75rem;
    border-radius: 2rem;
  }

  &__media {
    position: relative;
    display: flex;
    align-items: stretch;
    justify-content: stretch;
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

    &-header {
      display: flex;
      align-items: center;
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      background: linear-gradient(to top, rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.8));
      padding: 1rem 1rem 2rem;
      font-weight: 600;
      color: white;
      z-index: 11;
      overflow: hidden;
    }

    .lazy-image {
      width: 100%;
      height: 100%;
    }
  }

  &--image, .attachment__media--active {
    .attachment__media-header {
      transition: transform 0.2s ease-in-out;
      transform: translateY(-100%);
    }

    .attachment__content:hover .attachment__media-header, &.attachment__media--active:hover .attachment__media-header {
      transform: translateY(0);
    }
  }
}
</style>
