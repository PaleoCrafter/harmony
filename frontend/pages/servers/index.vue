<template>
  <div class="servers-index">
    Select a server from the list to the left.
  </div>
</template>

<style lang="scss">
.servers-index {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
  flex-direction: column;
  padding: 1rem;

  @media (max-width: 767px) {
    display: none;
  }
}
</style>
