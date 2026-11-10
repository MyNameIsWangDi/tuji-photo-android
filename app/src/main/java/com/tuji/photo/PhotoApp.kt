package com.tuji.photo

import android.app.Application
import com.tuji.photo.ui.theme.ThemeController
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhotoApp : Application() {

    companion object {
        var instance: PhotoApp? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeController.load()
    }
}
