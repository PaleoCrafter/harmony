<template>
  <div class="login">
    <div class="login__content">
      <KeyIcon size="15x" class="login__icon" />
      You need to log in in order to see this page. Press the button below to grant this application access to your Discord profile.
      <a :href="loginUrl" class="login__button">Authorize</a>
    </div>
  </div>
</template>

<script>
import { KeyIcon } from 'vue-feather-icons'

export default {
  auth: 'guest',
  components: { KeyIcon },
  computed: {
    loginUrl () {
      const { redirect } = this.$route.query
      const query = redirect ? `?redirect=${redirect}` : ''

      return `/api/auth/login${query}`
    }
  }
}
</script>

<style lang="scss">
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  flex-grow: 1;
  font-size: 1.1rem;
  position: relative;

  &__icon {
    position: absolute;
    bottom: 100%;
    color: rgba(0, 0, 0, 0.1);
  }

  &__content {
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    flex-direction: column;
  }

  &__button {
    position: relative;
    color: #fff;
    background: #7289da;
    cursor: pointer;
    text-decoration: none;
    border: none;
    font-size: 1rem;
    border-radius: 3px;
    padding: 0.5rem 1rem;
    margin-top: 1rem;
    z-index: 1;

    &:hover, &:active, &:focus {
      background-color: #677bc4;
    }
  }
}
</style>
