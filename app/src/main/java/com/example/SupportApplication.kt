package com.example

import android.app.Application
import android.database.CursorWindow

class SupportApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 100 * 1024 * 1024) // 100 MB buffer for large SQLite queries
        } catch (_: Throwable) {}
    }
}
