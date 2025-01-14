package com.advice.schedule.ui.events

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.advice.schedule.Response
import com.advice.schedule.database.DatabaseManager
import com.advice.schedule.database.ReminderManager
import com.advice.schedule.getTintedDrawable
import com.advice.schedule.models.local.Event
import com.advice.schedule.models.local.LocationContainer
import com.advice.schedule.models.local.toColour
import com.advice.schedule.ui.activities.MainActivity
import com.advice.schedule.ui.information.locations.LocationsViewModel
import com.advice.schedule.ui.information.locations.toContainer
import com.advice.schedule.ui.information.speakers.SpeakersAdapter
import com.advice.schedule.ui.schedule.ScheduleViewModel
import com.advice.schedule.utilities.Analytics
import com.advice.schedule.utilities.Storage
import com.advice.schedule.utilities.TimeUtil
import com.advice.schedule.utilities.getLocalizedDate
import com.shortstack.hackertracker.R
import com.shortstack.hackertracker.databinding.FragmentEventBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class EventFragment : Fragment() {

    private var _binding: FragmentEventBinding? = null
    private val binding get() = _binding!!

    private val analytics by inject<Analytics>()
    private val database by inject<DatabaseManager>()
    private val reminder by inject<ReminderManager>()
    private val storage by inject<Storage>()

    private val viewModel by sharedViewModel<ScheduleViewModel>()
    private val locationsViewModel by sharedViewModel<LocationsViewModel>()

    private lateinit var speakersAdapter: SpeakersAdapter
    private val linksAdapter = EventDetailsAdapter()

    private lateinit var event: Event

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        event = arguments?.getParcelable(EXTRA_EVENT) ?: error("event must not be null")

        speakersAdapter = SpeakersAdapter {
            (requireActivity() as MainActivity).showSpeaker(it)
        }

        showEvent(event)

        viewModel.getSchedule().observe(viewLifecycleOwner) {
            if (it is Response.Success) {
                val target = it.data?.find { it.id == event.id }
                if (target != null) {
                    showEvent(target)
                }
            }
        }

        binding.hint.isVisible = storage.tutorialEventLocations
        binding.hint.setOnCloseListener {
            storage.tutorialEventLocations = false
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        if (event.isBookmarked) {
            inflater.inflate(R.menu.event_bookmarked, menu)
        } else {
            inflater.inflate(R.menu.event_unbookmarked, menu)
        }
        // Only showing the share for DEFCON30
        menu.getItem(1).isVisible = database.conference.value?.code == "DEFCON30"
    }

    private fun showEvent(event: Event) {
        this.event = event
        analytics.log("Viewing event: ${event.title}")

        val body = event.description

        binding.speakers.adapter = speakersAdapter
        binding.links.adapter = linksAdapter

        binding.speakersHeader.isVisible = event.speakers.isNotEmpty()
        binding.speakersContainers.isVisible = event.speakers.isNotEmpty()
        speakersAdapter.submitList(event.speakers)
        binding.linksContainers.isVisible = event.urls.isNotEmpty()
        linksAdapter.setElements(event.urls.sortedBy { it.label.length }, emptyList())

        if (body.isNotBlank()) {
            binding.empty.visibility = View.GONE
            binding.description.text = body
        } else {
            binding.empty.visibility = View.VISIBLE
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_bookmark -> {
                    onBookmarkClick(event)
                }
                R.id.action_share -> {
                    onShareClick(event)
                }
            }

            true
        }

        displayDescription(event)

        displayTypes(event)

        displayBookmark(event)


        analytics.onEventView(event)
    }

    private fun onShareClick(event: Event) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, event.title)
            putExtra(Intent.EXTRA_TEXT, "https://info.defcon.org/events/${event.id}/")
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun onBookmarkClick(event: Event) {
        event.isBookmarked = !event.isBookmarked

        database.updateBookmark(event)
        if (event.isBookmarked) {
            reminder.setReminder(event)
        } else {
            reminder.cancel(event)
        }

        analytics.onEventBookmark(event)

        binding.toolbar.invalidate()
    }

    private fun displayBookmark(event: Event) {
        val isBookmarked = event.isBookmarked
        binding.toolbar.menu.clear()
        binding.toolbar.inflateMenu(if (isBookmarked) R.menu.event_bookmarked else R.menu.event_unbookmarked)
    }

    private fun displayDescription(event: Event) {
        binding.title.text = event.title
        binding.date.text = getFullTimeStamp(requireContext(), event).replace("\n", " ")

        updateLocationStatus(event.location.toContainer())

        // fetching the updated location status
        locationsViewModel.updatedSchedule(event.location).observe(viewLifecycleOwner) { location ->
            updateLocationStatus(location)
        }

        binding.locationContainer.setOnClickListener {
            (requireActivity() as MainActivity).showSchedule(event.location)
        }
    }

    private fun updateLocationStatus(location: LocationContainer) {
        binding.location.setLocation(location, useShortLabel = false, showDot = false)

        val drawable = requireContext().getTintedDrawable(R.drawable.ic_map_white_24dp, location.status.toColour())
        binding.locationIcon.setImageDrawable(drawable)
    }

    private fun getFullTimeStamp(context: Context, event: Event): String {
        val (begin, end) = getTimeStamp(context, event)
        val timestamp = TimeUtil.getDateStamp(getLocalizedDate(event.start.toDate()))

        return String.format(context.getString(R.string.timestamp_full), timestamp, begin, end)
    }

    private fun getTimeStamp(context: Context, event: Event): Pair<String, String> {
        val begin = TimeUtil.getTimeStamp(context, getLocalizedDate(event.start.toDate()))
        val end = TimeUtil.getTimeStamp(context, getLocalizedDate(event.end.toDate()))
        return Pair(begin, end)
    }


    private fun displayTypes(event: Event) {
        val types = event.types.take(3)
        val views = listOf(binding.type1, binding.type2, binding.type3)

        // handling conferences without types
        if (types.isEmpty()) {
            views.forEach {
                it.isVisible = false
            }
            return
        }

        for (i in 0 until 3) {
            if (i < types.size) {
                views[i].isVisible = true
                views[i].render(types[i])
                views[i].setOnClickListener {
                    (requireActivity() as MainActivity).showSchedule(types[i])
                }
            } else {
                views[i].isVisible = false
            }
        }
    }

    companion object {

        const val EXTRA_EVENT = "EXTRA_EVENT"

        fun newInstance(event: Event): EventFragment {
            val fragment = EventFragment()

            val bundle = Bundle()
            bundle.putParcelable(EXTRA_EVENT, event)
            fragment.arguments = bundle

            return fragment
        }
    }
}