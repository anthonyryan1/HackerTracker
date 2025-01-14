package com.advice.schedule.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.advice.schedule.database.DatabaseManager
import com.advice.schedule.database.ReminderManager
import com.advice.schedule.models.local.Event
import com.advice.schedule.ui.activities.MainActivity
import com.advice.schedule.utilities.Analytics
import com.shortstack.hackertracker.R
import com.shortstack.hackertracker.databinding.RowEventBinding
import org.koin.core.KoinComponent
import org.koin.core.inject

class EventView : FrameLayout, KoinComponent {

    private val binding = RowEventBinding.inflate(LayoutInflater.from(context), this, true)

    // todo: extract
    private val analytics: Analytics by inject()
    private val database: DatabaseManager by inject()
    private val reminder: ReminderManager by inject()

    var displayMode: Int = DISPLAY_MODE_MIN

    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        display: Int = DISPLAY_MODE_MIN
    ) : super(context, attrs) {
        displayMode = display
        init()
    }

    constructor(context: Context, event: Event, display: Int = DISPLAY_MODE_FULL) : super(context) {
        displayMode = display
        init()
        setContent(event)
    }

    private fun init() {
        setDisplayMode()
    }

    fun setContent(event: Event) {
        render(event)
    }

    private fun setDisplayMode() {
        when (displayMode) {
            DISPLAY_MODE_MIN -> {
                val width = context.resources.getDimension(R.dimen.event_view_min_guideline).toInt()
                binding.guideline.setGuidelineBegin(width)
                binding.typesContainer.visibility = View.GONE
            }
            DISPLAY_MODE_FULL -> {
                val width = context.resources.getDimension(R.dimen.time_width).toInt()
                binding.guideline.setGuidelineBegin(width)
                binding.typesContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun render(event: Event) {
        binding.title.text = event.title

        // Stage 2
        if (displayMode == DISPLAY_MODE_FULL) {
            binding.location.text = event.location.name
        } else {
            binding.location.text = event.getFullTimeStamp(context) + " / " + event.location.name
        }


        renderCategoryColour(event)
        updateBookmark(event)

        setOnClickListener {
            (context as? MainActivity)?.showEvent(event)
        }

        binding.starBar.setOnClickListener {
            onBookmarkClick(event)
        }
    }

    private fun renderCategoryColour(event: Event) {
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
            // showing the side bar colour
            if (i == 0) {
                val value = TypedValue()
                context.theme.resolveAttribute(R.attr.category_tint, value, true)
                val id = value.resourceId

                val color = if (id > 0) {
                    ContextCompat.getColor(context, id)
                } else {
                    Color.parseColor(types[i].color)
                }
                binding.category.setBackgroundColor(color)
            }

            // rendering each type
            if (i < types.size) {
                views[i].isVisible = true
                views[i].render(types[i])
            } else {
                views[i].isVisible = false
            }
        }
    }

    private fun updateBookmark(event: Event) {
        val isBookmarked = event.isBookmarked

        val drawable = if (isBookmarked) {
            R.drawable.ic_star_accent_24dp
        } else {
            R.drawable.ic_star_border_white_24dp
        }

        binding.starBar.setImageResource(drawable)
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

        updateBookmark(event)
    }

    companion object {
        const val DISPLAY_MODE_MIN = 0
        const val DISPLAY_MODE_FULL = 1
    }
}
