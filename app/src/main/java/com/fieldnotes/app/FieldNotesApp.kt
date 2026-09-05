package com.fieldnotes.app

import android.app.Application
import com.fieldnotes.app.di.AppContainer

class FieldNotesApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
