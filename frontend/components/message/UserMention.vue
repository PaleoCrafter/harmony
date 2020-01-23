<script>
import userQuery from '@/apollo/queries/user.gql'
import UserName from '@/components/UserName.vue'

export default {
  name: 'UserMention',
  components: { UserName },
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
  },
  render (h) {
    if (this.user === null || this.user === undefined) {
      return h('span', { class: 'user-mention user-mention--unknown' }, [`@${this.id}`])
    }

    return h(UserName, { class: 'user-mention', props: { server: this.server, user: { id: this.id, ...this.user }, prefix: '@' } })
  }
}
</script>

<style lang="scss">
.user-mention {
  & > span, &--unknown {
    color: #7289da;
    background-color: rgba(114, 137, 218, .1);
    transition: background-color 50ms ease-out, color 50ms ease-out;
    cursor: pointer;
    vertical-align: baseline;
    border-radius: 0.125rem;
    padding: 0 0.125rem;

    &:hover {
      color: white;
      background-color: rgba(114, 137, 218, 0.7);
      text-decoration: none;
    }
  }
}
</style>
