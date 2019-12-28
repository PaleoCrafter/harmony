export default (error, context) => {
  if (error.networkError && error.networkError.statusCode === 401) {
    context.redirect(`/api/auth/login?redirect=${encodeURIComponent(context.route.fullPath)}`)
  } else {
    // eslint-disable-next-line no-console
    console.error(error)
    context.error({ statusCode: 500, message: 'Server error' })
  }
}
