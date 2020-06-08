<template>
  <div class="privacy-policy">
    <header class="privacy-policy__header">
      <a v-if="$store.state.loggedIn" href="#" class="privacy-policy__header-menu-toggle" @click.prevent="$store.commit('openSidebar')">
        <MenuIcon />
      </a>
      <h2>Privacy Policy</h2>
    </header>
    <article class="privacy-policy__content">
      <section>
        This privacy policy will explain how our organization uses the personal data we collect from you when you use our website.
      </section>

      <section>
        <h3>Topics</h3>

        <ul>
          <li><a href="#what-data">What data do we collect?</a></li>
          <li><a href="#data-collection">How do we collect your data?</a></li>
          <li><a href="#data-usage">How will we use your data?</a></li>
          <li><a href="#data-retention">Who can access your data for how long?</a></li>
          <li><a href="#rights">What are your data protection rights?</a></li>
          <li><a href="#cookies">What are cookies?</a></li>
          <li><a href="#cookie-usage">How do we use cookies?</a></li>
          <li><a href="#cookie-types">What types of cookies do we use?</a></li>
          <li><a href="#cookie-management">How to manage your cookies</a></li>
          <li><a href="#other-websites">Privacy policies of other websites</a></li>
          <li><a href="#changes">Changes to our privacy policy</a></li>
          <li><a href="#contact">How to contact us</a></li>
        </ul>
      </section>

      <section>
        <h3 id="what-data">
          What data do we collect?
        </h3>
        <p>Harmony collects the following data:</p>

        <ul>
          <li>
            Any messages submitted by you to Discord channels that are observed by Harmony, including references such as links to media,
            but not the actual media itself
          </li>
          <li>
            A list of Discord servers you are in, including the set of permissions you have on that server
          </li>
          <li>The timezone your system was in at the time of logging into the application</li>
        </ul>

        <p>
          As they are a matter of public record within the confines of a server, our collection of messages extends to deleted or edited
          ones as well as those in a deleted channel, for archival purposes.
        </p>
      </section>

      <section>
        <h3 id="data-collection">
          How do we collect your data?
        </h3>
        <p>
          You directly provide Harmony with some of the data we collect. We collect data and process data when you log into your Discord
          account via our application. By logging in, you explicitly give us consent to collect said data.
        </p>

        <p>
          Harmony also receives your data indirectly by automatically ingesting conversations in Discord channels you partake in,
          if the server and channel are configured to be archived by Harmony.
        </p>
      </section>

      <section>
        <h3 id="data-usage">
          How will we use your data?
        </h3>
        <p>Harmony collects your data so that we can fulfill the legitimate purposes of the application, namely:</p>

        <ul>
          <li>Provide a record of Discord channels even after their deletion for archiving purposes</li>
          <li>
            Run queries provided by other users on the messages in any Discord channel they have access to,
            which may include messages by you
          </li>
          <li>Display a list of relevant servers for you to view</li>
          <li>Allow moderators of Discord servers to make more informed decisions based on your message history</li>
          <li>Timezone data will be used to retrieve and display messages in timeframes relevant to you</li>
        </ul>

        <p>
          Apart from the initial Discord login process, none of your data is shared with any third parties.
          You may review <a href="https://discordapp.com/privacy" target="_blank">Discord's privacy policy</a> for further information how
          your data will be processed there.
        </p>
      </section>

      <section>
        <h3 id="data-retention">
          Who can access your data for how long?
        </h3>
        <p>
          Messages stored within Harmony can only be directly accessed by those whose Discord accounts have the appropriate permissions to
          view them at the current time. Deleted messages and a message history will only be visible to you and any user who has the
          "Manage messages" permission for the containing channel.
        </p>

        <p>
          Messages will be stored for as long as they are relevant to the purposes mentioned above. If that is no longer the case, they will
          be deleted from our databases.
        </p>

        <p>
          The information directly provided by you via logging into the application will be stored for a maximum of 30 days after your
          latest activity on the site. The information will be deleted upon the expiry of this period.
        </p>
      </section>

      <section>
        <h3 id="rights">
          What are your data protection rights?
        </h3>
        <p>
          Harmony would like to make sure you are fully aware of all of your data protection rights. Every user is entitled to the
          following:
        </p>

        <p><strong>The right to access</strong> – You have the right to request Harmony for copies of your personal data.</p>

        <p>
          <strong>The right to rectification</strong> – You have the right to request that Harmony correct any information you believe is
          inaccurate. You also have the right to request Harmony to complete the information you believe is incomplete.
        </p>

        <p>
          <strong>The right to erasure</strong> – You have the right to request that Harmony erase your personal data, under certain
          conditions.
        </p>

        <p>
          <strong>The right to restrict processing</strong> – You have the right to request that Harmony restrict the processing of your
          personal data, under certain conditions.
        </p>

        <p>
          <strong>The right to object to processing</strong> – You have the right to object to Harmony’s processing of your personal data,
          under certain conditions.
        </p>

        <p>
          <strong>The right to data portability</strong> – You have the right to request that Harmony transfer the data that we have
          collected to another organization, or directly to you, under certain conditions.
        </p>

        <p>
          If you make a request, we have one month to respond to you. If you would like to exercise any of these rights, please contact us
          at our email:
        </p>
      </section>

      <section>
        <h3 id="cookies">
          What are cookies?
        </h3>
        <p>
          Cookies are text files placed on your computer to collect standard Internet log information and visitor behavior information. When
          you visit our websites, we may collect information from you automatically through cookies or similar technology.
        </p>

        <p>
          For further information, visit <a href="https://allaboutcookies.org" target="_blank">allaboutcookies.org</a>.
        </p>
      </section>

      <section>
        <h3 id="cookie-usage">
          How do we use cookies?
        </h3>
        Harmony uses cookies to improve your experience on our website, namely for keeping you signed in across browser sessions and
        for temporarily storing information such as your timezone during the login process.
      </section>

      <section>
        <h3 id="cookie-types">
          What types of cookies do we use?
        </h3>
        There are a number of different types of cookies, however, our website uses:

        <ul>
          <li>
            Functionality – Harmony uses these cookies so that we recognize you on our website and remember your previously selected
            preferences. These could include what language you prefer and location you are in.
          </li>
        </ul>
      </section>

      <section>
        <h3 id="cookie-management">
          How to manage cookies
        </h3>
        You can set your browser not to accept cookies, and the above website tells you how to remove cookies from your browser. However, in
        a few cases, some of our website features may not function as a result.
      </section>

      <section>
        <h3 id="other-websites">
          Privacy policies of other websites
        </h3>
        The Harmony website contains links to other websites. Our privacy policy applies only to our website, so if you click on a link
        to another website, you should read their privacy policy.
      </section>

      <section>
        <h3 id="changes">
          Changes to our privacy policy
        </h3>
        <p>
          Harmony keeps its privacy policy under regular review and places any updates on this web page. This privacy policy was last
          updated on 6 March 2020.
        </p>
      </section>

      <section>
        <h3 id="contact">
          How to contact us
        </h3>
        <p>
          If you have any questions about Harmony’s privacy policy, the data we hold on you, or you would like to exercise one of your
          data protection rights, please do not hesitate to contact us.
        </p>

        Email us at: <a href="mailto:marvin@17thshard.com">marvin@17thshard.com</a>
      </section>
    </article>
  </div>
</template>

<script>
import { MenuIcon } from 'vue-feather-icons'

export default {
  auth: false,
  layout ({ store }) {
    return store.state.loggedIn ? 'servers' : 'default'
  },
  components: { MenuIcon },
  head () {
    return {
      title: 'Privacy Policy - Harmony'
    }
  }
}
</script>

<style lang="scss">
.privacy-policy {
  display: flex;
  align-items: stretch;
  justify-content: flex-start;
  flex-grow: 1;
  flex-direction: column;

  &__header {
    display: flex;
    align-items: center;
    box-shadow: 0 1px 0 rgba(4, 4, 5, 0.2), 0 1.5px 0 rgba(6, 6, 7, 0.05), 0 2px 0 rgba(4, 4, 5, 0.05);
    padding: 1rem;
    font-weight: 600;
    height: 4rem;
    line-height: 1;
    z-index: 51;

    h2 {
      font-size: 1rem;
    }

    &-menu-toggle {
      color: white;
      margin-right: 0.25rem;

      @media (min-width: 1200px) {
        display: none;
      }
    }

    @media (max-width: 767px) {
      padding-left: 0.5rem;
    }
  }

  &__content {
    padding: 1rem;
    flex-grow: 1;
    overflow-y: scroll;

    section {
      margin: 2rem 0;
      max-width: 900px;

      &:first-child {
        margin-top: 0;
      }

      &:last-child {
        margin-bottom: 0;
      }

      h3 {
        font-weight: 600;
        text-transform: uppercase;
        color: rgba(255, 255, 255, 0.6);
        font-size: 1.4rem;
      }

      p, ul {
        margin: 0.5rem 0;
      }
    }

    a {
      color: #00b0f4;
      text-decoration: none;
      unicode-bidi: bidi-override;
      direction: ltr;
      word-break: break-word;

      &:hover, &:focus {
        text-decoration: underline;
      }
    }
  }
}
</style>
