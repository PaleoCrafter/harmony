<template>
  <div class="site">
    <nuxt />
  </div>
</template>

<style lang="scss">
html {
  font-family: 'Open Sans', sans-serif;
  font-size: 14px;
  -ms-text-size-adjust: 100%;
  -webkit-text-size-adjust: 100%;
  -moz-osx-font-smoothing: grayscale;
  -webkit-font-smoothing: antialiased;
  box-sizing: border-box;
}

*,
*:before,
*:after {
  box-sizing: border-box;
  margin: 0;
}

body {
  width: 100%;
  height: 100%;
  min-height: 100vh;
  background: #36393F;
  color: white;
  overflow: hidden;
}

.site {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  justify-items: stretch;
}
</style>
