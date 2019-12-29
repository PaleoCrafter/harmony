<template>
  <span class="user-mention">@{{ userName }}</span>
</template>

<script>
import userQuery from '@/apollo/queries/user.gql'

export default {
  name: 'UserMention',
  props: {
    server: {
      type: String,
      required: true
    },
    id: {
      type: String,
      required: true
    }
  },
  computed: {
    userName () {
      return this.user?.nickname ?? this.user?.name ?? this.id
    }
  },
  apollo: {
    user: {
      query: userQuery,
      variables () {
        return {
          server: this.server,
          id: this.id
        }
      }
    }
  }
}
</script>

<style lang="scss">
.user-mention {
  color: #7289da;
  background-color: rgba(114, 137, 218, .1);
  transition: background-color 50ms ease-out, color 50ms ease-out;
  cursor: pointer;

  &:hover {
    color: white;
    background-color: rgba(114, 137, 218, 0.7);
  }
}
</style>
