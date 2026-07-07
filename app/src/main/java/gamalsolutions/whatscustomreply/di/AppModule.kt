package gamalsolutions.whatscustomreply.di

import androidx.room.Room
import gamalsolutions.whatscustomreply.data.api.CustomApiRepository
import gamalsolutions.whatscustomreply.data.database.AppDatabase
import gamalsolutions.whatscustomreply.data.datastore.SettingsManager
import gamalsolutions.whatscustomreply.data.repository.LogsRepository
import gamalsolutions.whatscustomreply.data.repository.RepliesRepository
import gamalsolutions.whatscustomreply.data.security.EncryptedPrefsManager
import gamalsolutions.whatscustomreply.ui.viewmodel.MainViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "auto_reply_db"
        ).fallbackToDestructiveMigration().build()
    }

    // DAOs
    single { get<AppDatabase>().customReplyDao() }
    single { get<AppDatabase>().autoReplyLogDao() }
    single { get<AppDatabase>().systemEventDao() }

    // Repositories
    single { RepliesRepository(get()) }
    single { gamalsolutions.whatscustomreply.data.repository.SystemEventsRepository(get()) }
    single { LogsRepository(get(), get()) }

    // Datastore and Security SharedPreferences
    single { SettingsManager(androidContext()) }
    single { EncryptedPrefsManager(androidContext()) }

    // General OkHttpClient for Custom API Requests
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Repository for Custom API Client
    single { CustomApiRepository(get()) }

    // Repository for Gemini API Client
    single { gamalsolutions.whatscustomreply.data.api.GeminiRepository(get()) }

    // Speech synthesis helper
    single { gamalsolutions.whatscustomreply.service.SpeechHelper(androidContext()) }

    // Interactive voice calling dialogue helper
    single { gamalsolutions.whatscustomreply.service.InteractiveVoiceHelper(androidContext()) }

    // ViewModel
    viewModel { MainViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
