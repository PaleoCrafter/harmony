const routeOption = (route, key, value) => {
  return route.matched.some((m) => {
    if (process.client) {
      // Client
      return Object.values(m.components).some(
        component => component.options && component.options[key] === value
      )
    } else {
      // SSR
      return Object.values(m.components).some(component =>
        Object.values(component._Ctor).some(
          ctor => ctor.options && ctor.options[key] === value
        )
      )
    }
  })
}

const getMatchedComponents = (route, matches = false) => {
  return [].concat.apply([], route.matched.map(function (m, index) {
    return Object.keys(m.components).map(function (key) {
      matches && matches.push(index)
      return m.components[key]
    })
  }))
}

export default function ({ store, route, redirect }) {
  if (routeOption(route, 'auth', false)) {
    return
  }

  const matches = []
  const matchedComponents = getMatchedComponents(route, matches)
  if (!matchedComponents.length) {
    return
  }

  if (store.state.loggedIn && routeOption(route, 'auth', 'guest')) {
    if (route.query.redirect) {
      redirect(route.query.redirect)
    } else {
      redirect('/')
    }
  } else if (!store.state.loggedIn && !routeOption(route, 'auth', 'guest')) {
    redirect(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
  }
}
