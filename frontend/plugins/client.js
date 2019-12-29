import Vue from 'vue'
import InfiniteScroll from 'vue-infinite-scroll'
import { DatePicker } from 'v-calendar'

Vue.use(InfiniteScroll)
Vue.component('DatePicker', DatePicker)
