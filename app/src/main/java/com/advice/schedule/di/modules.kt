package com.advice.schedule.di

import androidx.work.WorkManager
import com.advice.schedule.PreferenceViewModel
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.advice.schedule.database.DatabaseManager
import com.advice.schedule.database.ReminderManager
import com.advice.schedule.ui.HackerTrackerViewModel
import com.advice.schedule.ui.home.HomeViewModel
import com.advice.schedule.ui.information.faq.FAQViewModel
import com.advice.schedule.ui.information.locations.LocationsViewModel
import com.advice.schedule.ui.information.speakers.SpeakersViewModel
import com.advice.schedule.ui.information.vendors.VendorsViewModel
import com.advice.schedule.ui.schedule.ScheduleViewModel
import com.advice.schedule.ui.settings.SettingsViewModel
import com.advice.schedule.ui.themes.ThemesManager
import com.advice.schedule.utilities.Analytics
import com.advice.schedule.utilities.NotificationHelper
import com.advice.schedule.utilities.Storage
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { Storage(get(), get()) }
    single { NotificationHelper(get()) }
    single {
        GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    }
    single { FirebaseJobDispatcher(GooglePlayDriver(get())) }
    single { DatabaseManager(get(), get()) }
    single { ThemesManager(get()) }

    single { FirebaseCrashlytics.getInstance() }

    single { Analytics(get()) }
    single { WorkManager.getInstance() }
    single { ReminderManager(get(), get()) }


    viewModel { HomeViewModel() }
    viewModel { HackerTrackerViewModel() }
    viewModel { PreferenceViewModel() }
    viewModel { ScheduleViewModel() }
    viewModel { SpeakersViewModel() }
    viewModel { LocationsViewModel() }
    viewModel { VendorsViewModel() }
    viewModel { FAQViewModel() }
    viewModel { SettingsViewModel() }

}