<script>
export default {
  name: 'LoadingSpinner',
  functional: true,
  render (h, context) {
    return h('div', { ...context.data, class: ['loading-spinner', context.data.class] }, [
      h('div', { class: 'loading-spinner__cube1' }),
      h('div', { class: 'loading-spinner__cube2' })
    ])
  }
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

@keyframes sk-cubemove {
  25% {
    transform: translateX(1em) rotate(-90deg) scale(0.5);
  }
  50% {
    transform: translateX(1em) translateY(1em) rotate(-179deg);
  }
  50.1% {
    transform: translateX(1em) translateY(1em) rotate(-180deg);
  }
  75% {
    transform: translateX(0px) translateY(1em) rotate(-270deg) scale(0.5);
  }
  100% {
    transform: rotate(-360deg);
  }
}
</style>
