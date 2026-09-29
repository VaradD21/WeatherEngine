package com.weatherengine.app

import android.app.Application
import com.weatherengine.app.data.di.AppContainer

class WeatherEngineApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
