package com.team7.socialblind

import android.app.Application
import android.content.Context
import android.content.res.Configuration

import timber.log.Timber

import com.team7.socialblind.util.LocaleManager


class ElbessApplication : Application() {



    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

}