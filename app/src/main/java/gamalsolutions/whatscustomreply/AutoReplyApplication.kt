package gamalsolutions.whatscustomreply

import android.app.Application
import gamalsolutions.whatscustomreply.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AutoReplyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AutoReplyApplication)
            modules(appModule)
        }
    }
}
