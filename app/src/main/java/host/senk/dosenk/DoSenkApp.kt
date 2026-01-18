package host.senk.dosenk

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DoSenkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        //  aunqe el cel esté en modo oscurop, la app usa los colres
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}