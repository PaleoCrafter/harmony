<template>
  <div class="loading-spinner">
    <div class="loading-spinner__cube1" />
    <div class="loading-spinner__cube2" />
  </div>
</template>

<script>
export default {
  name: 'LoadingSpinner'
}
</script>

<style lang="scss">
.loading-spinner {
  width: 1em;
  height: 1em;
  position: relative;

  &__cube1, &__cube2 {
    background-color: #7289DA;
    width: 0.375em;
    height: 0.375em;
    position: absolute;
    top: 0;
    left: 0;

    animation: sk-cubemove 1.8s infinite ease-in-out;
  }

  &__cube2 {
    -webkit-animation-delay: -0.9s;
    animation-delay: -0.9s;
  }
}

@-webkit-keyframes sk-cubemove {
  25% {
    -webkit-transform: translateX(42px) rotate(-90deg) scale(0.5)
  }
  50% {
    -webkit-transform: translateX(42px) translateY(42px) rotate(-180deg)
  }
  75% {
    -webkit-transform: translateX(0px) translateY(42px) rotate(-270deg) scale(0.5)
  }
  100% {
    -webkit-transform: rotate(-360deg)
  }
}

@keyframes sk-cubemove {
  25% {
    transform: translateX(42px) rotate(-90deg) scale(0.5);
    -webkit-transform: translateX(42px) rotate(-90deg) scale(0.5);
  }
  50% {
    transform: translateX(42px) translateY(42px) rotate(-179deg);
    -webkit-transform: translateX(42px) translateY(42px) rotate(-179deg);
  }
  50.1% {
    transform: translateX(42px) translateY(42px) rotate(-180deg);
    -webkit-transform: translateX(42px) translateY(42px) rotate(-180deg);
  }
  75% {
    transform: translateX(0px) translateY(42px) rotate(-270deg) scale(0.5);
    -webkit-transform: translateX(0px) translateY(42px) rotate(-270deg) scale(0.5);
  }
  100% {
    transform: rotate(-360deg);
    -webkit-transform: rotate(-360deg);
  }
}
</style>
