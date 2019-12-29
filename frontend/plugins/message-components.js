import Vue from 'vue'
import Spoiler from '@/components/message/Spoiler.vue'
import UserMention from '@/components/message/UserMention.vue'
import ChannelMention from '@/components/message/ChannelMention.vue'
import RoleMention from '@/components/message/RoleMention.vue'
import GenericMention from '@/components/message/GenericMention.vue'

Vue.component('Spoiler', Spoiler)
Vue.component('UserMention', UserMention)
Vue.component('ChannelMention', ChannelMention)
Vue.component('RoleMention', RoleMention)
Vue.component('GenericMention', GenericMention)
