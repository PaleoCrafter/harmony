<template>
  <div class="user-info">
    <div class="user-info__header">
      <span class="user-info__name">{{ user.name }}</span>
      <span class="user-info__discriminator">#{{ user.discriminator }}</span>
    </div>
    <div class="user-info__details">
      <LoadingSpinner v-if="$apollo.loading" />
      <template v-if="details && details.roles.length > 0">
        <strong>Roles</strong>
        <div class="user-info__roles">
          <span v-for="role in details.roles" :key="role.name" :style="roleStyle(role)" class="user-info__role">
            {{ role.name }}
          </span>
        </div>
      </template>
      <template v-if="details && details.nicknames.length > 0">
        <strong>Nickname History</strong>
        <div class="user-info__nicknames">
          <div v-for="nickname in details.nicknames" :key="nickname.timestamp" class="user-info__nickname">
            <span>{{ nickname.name }}</span>
            <time :datetime="nickname.timestamp">{{ formatTimestamp(nickname.timestamp) }}</time>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import detailsQuery from '@/apollo/queries/user-details.gql'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

export default {
  name: 'UserInfo',
  components: { LoadingSpinner },
  props: {
    server: {
      type: String,
      required: true
    },
    user: {
      type: Object,
      required: true
    }
  },
  apollo: {
    details: {
      query: detailsQuery,
      variables () {
        return {
          server: this.server,
          id: this.user.id
        }
      },
      update: data => data.userDetails
    }
  },
  methods: {
    roleStyle (role) {
      const defaultColor = [200, 200, 200]
      let { r, g, b } = role.color

      if (r === 0 && g === 0 && b === 0) {
        [r, g, b] = defaultColor
      }

      return {
        '--role-color': `rgb(${r}, ${g}, ${b})`
      }
    },
    formatTimestamp (timestamp) {
      const format = new Intl.DateTimeFormat(undefined, {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
      })
      return format.format(Date.parse(timestamp))
    }
  }
}
</script>

<style lang="scss">
.user-info {
  overflow: hidden;
  flex-direction: column;
  align-items: stretch;
  border-radius: 0.4rem;

  &__header {
    align-items: center;
    justify-content: center;
    background: #202225;
    padding: 0.75rem 1rem;
    display: flex;
  }

  &__name {
    color: white;
    font-weight: 600;
  }

  &__discriminator {
    color: rgba(255, 255, 255, 0.6);
  }

  &__details {
    background: #2f3136;
    padding: 0.75rem 0.75rem;

    strong {
      font-weight: 700;
      font-size: 0.9rem;
      text-transform: uppercase;
      line-height: 1;
      color: #72767d;
    }
  }

  &__roles {
    display: flex;
    flex-wrap: wrap;
    margin: 0 -0.25rem;
  }

  &__role {
    border: 1px solid var(--role-color);
    padding: 0.25rem 0.5rem 0.25rem 0.3rem;
    border-radius: 1rem;
    font-size: 0.8rem;
    line-height: 1rem;
    display: flex;
    align-items: center;
    margin: 0.25rem 0.25rem;

    &:before {
      content: '';
      width: 0.8rem;
      height: 0.8rem;
      background: var(--role-color);
      border-radius: 100%;
      margin-right: 0.25rem;
    }
  }

  &__nicknames {
    max-height: 150px;
    overflow-y: auto;
  }

  &__nickname {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    padding: 0.25rem 0;

    span {
      font-size: 0.9rem;
    }

    time {
      font-size: 0.8rem !important;
    }
  }

  ::-webkit-scrollbar {
    width: 0.5rem;
    height: 0.5rem;
  }

  ::-webkit-scrollbar-corner {
    background-color: transparent;
  }

  ::-webkit-scrollbar-track {
    border-width: initial;
    background-color: transparent;
    border-color: transparent;
  }

  ::-webkit-scrollbar-track, ::-webkit-scrollbar-thumb {
    background-clip: padding-box;
    border-width: 2px;
    border-style: solid;
    border-radius: 3px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: #202225;
    border: 1px solid #36393f;
  }

  ::-webkit-scrollbar-track-piece {
    background-color: #2f3136;
    border: 1px solid #36393f;
    border-radius: 7px;
  }
}
</style>
