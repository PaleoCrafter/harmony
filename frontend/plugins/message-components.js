import Vue from 'vue'
import hljs from 'highlight.js/lib/highlight'
import Spoiler from '@/components/message/Spoiler.vue'
import UserMention from '@/components/message/UserMention.vue'
import ChannelMention from '@/components/message/ChannelMention.vue'
import RoleMention from '@/components/message/RoleMention.vue'
import GenericMention from '@/components/message/GenericMention.vue'
import CodeBlock from '@/components/message/CodeBlock.vue'

Vue.component('Spoiler', Spoiler)
Vue.component('UserMention', UserMention)
Vue.component('ChannelMention', ChannelMention)
Vue.component('RoleMention', RoleMention)
Vue.component('GenericMention', GenericMention)
Vue.component('CodeBlock', CodeBlock)
Vue.directive('hljs', {
  deep: true,
  bind (el, binding) {
    const targets = el.querySelectorAll('code')
    targets.forEach((target) => {
      hljs.highlightBlock(target)
    })
  },
  componentUpdated (el, binding) {
    const targets = el.querySelectorAll('code')
    targets.forEach((target) => {
      if (binding.value) {
        hljs.highlightBlock(target)
      }
    })
  }
})
