<template>
  <div class="index">
    <div class="index__content">
      <KeyIcon size="15x" class="index__icon" />
      You need to log in in order to use this application.
      <nuxt-link to="/login?redirect=/servers" class="index__button">
        Log in now
      </nuxt-link>
    </div>
    <footer>
      <nuxt-link to="/privacy-policy">
        Privacy Policy
      </nuxt-link>
    </footer>
  </div>
</template>

<script>
import { KeyIcon } from 'vue-feather-icons'

export default {
  auth: 'guest',
  components: { KeyIcon }
}
</script>

<style lang="scss">
.index {
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
    padding: 1rem;
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

  footer {
    display: flex;
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: center;
    padding: 1rem;

    a {
      color: rgba(255, 255, 255, 0.6);
      text-decoration: none;

      &:hover, &:active, &:focus {
        text-decoration: underline;
      }
    }
  }
}
</style>
