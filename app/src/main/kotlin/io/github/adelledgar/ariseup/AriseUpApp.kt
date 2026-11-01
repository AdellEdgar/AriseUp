package io.github.adelledgar.ariseup

import android.app.Application
import io.github.adelledgar.ariseup.di.AppContainer

class AriseUpApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
