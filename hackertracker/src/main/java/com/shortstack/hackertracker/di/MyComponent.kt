package com.shortstack.hackertracker.di

import com.shortstack.hackertracker.ui.vendors.VendorsFragment
import dagger.Component

/**
 * Created by Chris on 5/21/2018.
 */
@Component(modules = arrayOf(ContextModule::class, DatabaseModule::class, SharedPreferencesModule::class))
@MyApplicationScope
interface MyComponent {

    fun inject(vendorsFragment: VendorsFragment)
}