export default (error, context) => {
  if (!error.networkError || error.networkError.statusCode !== 401) {
    // eslint-disable-next-line no-console
    console.error(error)
  }
  if (context.req.handledApolloError) {
    return
  }
  if (error.networkError && error.networkError.statusCode === 401) {
    context.redirect(`/api/auth/login?redirect=${encodeURIComponent(context.route.fullPath)}`)
  } else {
    context.error({ statusCode: 500, message: 'Server error' })
  }
  context.req.handledApolloError = true
}
