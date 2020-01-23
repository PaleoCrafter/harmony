<script>
import { utcToZonedTime } from 'date-fns-tz'
import messageQuery from '@/apollo/queries/redirect-message.gql'

export default {
  async fetch ({ app: { apolloProvider: { defaultClient: apollo } }, params: { id }, redirect, error, store }) {
    try {
      const { data: { redirectMessage } } = await apollo.query({
        query: messageQuery,
        variables: {
          id
        }
      })

      if (redirectMessage !== null) {
        const date = utcToZonedTime(parseInt(redirectMessage.createdAt), store.state.timezone)
        const year = date.getFullYear().toString()
        const month = (date.getMonth() + 1).toString()
        const day = date.getDate().toString()
        const paddedMonth = month.length === 1 ? `0${month}` : month
        const paddedDay = day.length === 1 ? `0${day}` : day
        const isoDate = `${year}-${paddedMonth}-${paddedDay}`
        redirect(302, `/servers/${redirectMessage.server}/channels/${redirectMessage.channel}/${isoDate}?message=${id}`)
      } else {
        error(404, 'Message could not be found')
      }
    } catch {
      error(404, 'Message could not be found')
    }
  }
}
</script>
