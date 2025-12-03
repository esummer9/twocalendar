package com.ediapp.twocalendar

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 앱이 시작될 때 Firebase를 초기화합니다.
        FirebaseApp.initializeApp(this)
    }
}
