package gamalsolutions.whatscustomreply.di

import androidx.room.Room
import gamalsolutions.whatscustomreply.data.api.GeminiApiService
import gamalsolutions.whatscustomreply.data.api.GeminiRepository
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
import retrofit2.Retrofit
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

    // Repositories
    single { RepliesRepository(get()) }
    single { LogsRepository(get()) }

    // Datastore and Security SharedPreferences
    single { SettingsManager(androidContext()) }
    single { EncryptedPrefsManager(androidContext()) }

    // Retrofit & Network Client
    single {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(get())
            .build()
    }

    single { get<Retrofit>().create(GeminiApiService::class.java) }

    single { GeminiRepository(get(), get()) }

    // ViewModel
    viewModel { MainViewModel(get(), get(), get(), get(), get()) }
}
