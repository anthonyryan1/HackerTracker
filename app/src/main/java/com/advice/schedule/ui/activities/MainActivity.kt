package com.advice.schedule.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.advice.schedule.get
import com.advice.schedule.models.firebase.FirebaseTag
import com.advice.schedule.models.local.Event
import com.advice.schedule.models.local.Location
import com.advice.schedule.models.local.Speaker
import com.advice.schedule.replaceFragment
import com.advice.schedule.ui.PanelsFragment
import com.advice.schedule.ui.events.EventFragment
import com.advice.schedule.ui.information.InformationFragment
import com.advice.schedule.ui.information.speakers.SpeakerFragment
import com.advice.schedule.ui.maps.MapsFragment
import com.advice.schedule.ui.schedule.ScheduleFragment
import com.advice.schedule.ui.schedule.ScheduleViewModel
import com.advice.schedule.ui.settings.SettingsFragment
import com.advice.schedule.utilities.Analytics
import com.github.stkent.amplify.tracking.Amplify
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.shortstack.hackertracker.BuildConfig
import com.shortstack.hackertracker.R
import com.shortstack.hackertracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener,
    FragmentManager.OnBackStackChangedListener, KoinComponent {

    private val analytics by inject<Analytics>()

    private val viewModel by viewModel<ScheduleViewModel>()

    private lateinit var binding: ActivityMainBinding

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val map = HashMap<Int, Fragment>()

    private var secondaryVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (savedInstanceState == null) {
            if (Amplify.getSharedInstance().shouldPrompt() && !BuildConfig.DEBUG) {
                val review = ReviewBottomSheet.newInstance()
                review.show(this.supportFragmentManager, review.tag)
            }

            setMainFragment(R.id.nav_home, getString(R.string.home), false)
        }

        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val target = when {
            intent == null -> null
            intent.hasExtra("target") -> intent.getLongExtra("target", -1L)
            intent.data != null -> {
                intent.data.toString().substringAfter("events/").take(5).toLongOrNull()
            }
            else -> null
        }

        Timber.d("target: $target")
        if (target != null) {
            lifecycleScope.launch {
                val event = viewModel.getEventById(target)
                if (event != null) {
                    showEvent(event)
                } else {
                    Timber.e("Could not find event by id: $target")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.signInAnonymously().addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Timber.d("Successfully signed in. ${it.result}")
            } else {
                Timber.e("Could not sign in.")
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setMainFragment(item.itemId, item.title.toString(), false)
        binding.drawerLayout.closeDrawers()
        return true
    }

    override fun onBackStackChanged() {
        val fragments = supportFragmentManager.fragments
        val last = fragments.lastOrNull()

        if (last is EventFragment || last is SpeakerFragment) {
            secondaryVisible = true
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.main.container.visibility = View.INVISIBLE
        } else {
            secondaryVisible = false
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            binding.main.container.visibility = View.VISIBLE

            val panels = fragments.get(PanelsFragment::class.java)
            panels.invalidate()
        }
    }

    private fun getFragment(id: Int): Fragment {
        // TODO: Remove, this is a hacky solution for caching issue with InformationFragment's children fragments.
        if (id == R.id.nav_information)
            return InformationFragment.newInstance()

        if (id == R.id.nav_map)
            return MapsFragment.newInstance()

        if (id == R.id.nav_home)
            return PanelsFragment()

        if (map[id] == null) {
            map[id] = when (id) {
                R.id.nav_map -> MapsFragment.newInstance()
                R.id.nav_settings -> SettingsFragment.newInstance()
                else -> InformationFragment.newInstance()
            }
        }
        return map[id]!!
    }

    fun showEvent(event: Event) {
        setAboveFragment(EventFragment.newInstance(event))
        analytics.setScreen(Analytics.SCREEN_EVENT)
    }

    fun showSpeaker(speaker: Speaker?) {
        speaker ?: return
        setAboveFragment(SpeakerFragment.newInstance(speaker))
        analytics.setScreen(Analytics.SCREEN_SPEAKER)
    }

    fun showInformation() {
        setAboveFragment(InformationFragment.newInstance())
        analytics.setScreen(Analytics.SCREEN_INFORMATION)
    }

    fun showMap() {
        setAboveFragment(MapsFragment.newInstance())
        analytics.setScreen(Analytics.SCREEN_MAPS)
    }

    fun showSettings() {
        setAboveFragment(SettingsFragment.newInstance())
        analytics.setScreen(Analytics.SCREEN_SETTINGS)
    }

    fun showSchedule(location: Location) {
        setAboveFragment(ScheduleFragment.newInstance(location), hasAnimation = false)
        analytics.setScreen(Analytics.SCREEN_SCHEDULE)
    }

    fun showSchedule(type: FirebaseTag) {
        setAboveFragment(ScheduleFragment.newInstance(type), hasAnimation = false)
        analytics.setScreen(Analytics.SCREEN_SCHEDULE)
    }

    fun showSchedule(speaker: Speaker) {
        setAboveFragment(ScheduleFragment.newInstance(speaker), hasAnimation = false)
        analytics.setScreen(Analytics.SCREEN_SCHEDULE)
    }

    private fun setMainFragment(id: Int, title: String? = null, addToBackStack: Boolean) {
        replaceFragment(getFragment(id), R.id.container, backStack = addToBackStack)

        title?.let {
            supportActionBar?.title = it
        }
    }

    fun setAboveFragment(fragment: Fragment, hasAnimation: Boolean = true) {
        replaceFragment(
            fragment,
            R.id.container_above,
            hasAnimation = hasAnimation
        )
    }
}
