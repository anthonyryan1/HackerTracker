package com.shortstack.hackertracker.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.State
import androidx.work.WorkManager
import com.crashlytics.android.answers.CustomEvent
import com.shortstack.hackertracker.App
import com.shortstack.hackertracker.R
import com.shortstack.hackertracker.Status
import com.shortstack.hackertracker.analytics.AnalyticsController
import com.shortstack.hackertracker.database.DatabaseManager
import com.shortstack.hackertracker.ui.schedule.list.ScheduleAdapter
import com.shortstack.hackertracker.utils.TickTimer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_schedule.*
import kotlinx.android.synthetic.main.view_empty.view.*
import javax.inject.Inject


class ScheduleFragment : Fragment(){

    private val adapter: ScheduleAdapter = ScheduleAdapter()

    @Inject
    lateinit var database: DatabaseManager

    @Inject
    lateinit var timer: TickTimer

    private var disposable: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        App.application.component.inject(this)

        list.adapter = adapter

        val scheduleViewModel = ViewModelProviders.of(this).get(ScheduleViewModel::class.java)
        scheduleViewModel.schedule.observe(this, Observer {
            hideViews()

            if( it != null ) {
                adapter.state = it.status

                when (it.status) {
                    Status.SUCCESS -> {
                        adapter.setSchedule(it.data)
                        if (adapter.isEmpty()) {
                            showEmptyView()
                        }
                    }
                    Status.ERROR -> {
                        showErrorView(it.message)
                    }
                    Status.LOADING -> {
                        adapter.clearAndNotify()
                        showProgress()
                    }
                    Status.NOT_INITIALIZED -> {
                        showEmptyView()
                    }
                }
            }
        })
    }


    override fun onResume() {
        super.onResume()

        disposable = timer.observable.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    adapter.notifyTimeChanged()
                    if (adapter.isEmpty()) {
                        showEmptyView()
                    } else {
                        hideViews()
                    }
                }
    }

    override fun onPause() {
        disposable?.dispose()
        disposable = null
        super.onPause()
    }

    private fun showProgress() {
        loading_progress.visibility = View.VISIBLE
    }

    private fun hideViews() {
        empty.visibility = View.GONE
        loading_progress.visibility = View.GONE
    }

    private fun showEmptyView() {
        empty.visibility = View.VISIBLE
    }

    private fun showErrorView(message: String?) {
        empty.title.text = message
        empty.visibility = View.VISIBLE
    }

    companion object {

        fun newInstance() = ScheduleFragment()

    }
}