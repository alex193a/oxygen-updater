package com.oxygenupdater

import android.app.NotificationManager
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.oxygenupdater.apis.DownloadApi
import com.oxygenupdater.apis.ServerApi
import com.oxygenupdater.database.NewsDatabaseHelper
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.repositories.BillingRepository
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.Database.buildLocalBillingDatabase
import com.oxygenupdater.utils.createDownloadClient
import com.oxygenupdater.utils.createNetworkClient
import com.oxygenupdater.utils.createOkHttpCache
import com.oxygenupdater.viewmodels.AboutViewModel
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.InstallViewModel
import com.oxygenupdater.viewmodels.MainViewModel
import com.oxygenupdater.viewmodels.NewsViewModel
import com.oxygenupdater.viewmodels.OnboardingViewModel
import com.oxygenupdater.viewmodels.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import retrofit2.Retrofit

private const val QUALIFIER_SERVER = "SERVER"
private const val QUALIFIER_DOWNLOAD = "DOWNLOAD"

private val retrofitModule = module {
    single(StringQualifier(QUALIFIER_SERVER)) { createNetworkClient(createOkHttpCache(androidContext())) }
    single(StringQualifier(QUALIFIER_DOWNLOAD)) { createDownloadClient() }
}

private val networkModule = module {
    single { get<Retrofit>(StringQualifier(QUALIFIER_SERVER)).create(ServerApi::class.java) }
    single { get<Retrofit>(StringQualifier(QUALIFIER_DOWNLOAD)).create(DownloadApi::class.java) }
}

private val preferencesModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
    single { SettingsManager() }
}

private val repositoryModule = module {
    single { ServerRepository(get(), get(), get(), get()) }
    single { BillingRepository(get(), get()) }
}

private val viewModelModule = module {
    viewModel { OnboardingViewModel(get(), get(), get()) }
    viewModel { MainViewModel(get()) }
    viewModel { NewsViewModel(get()) }
    viewModel { InstallViewModel(get()) }
    viewModel { AboutViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { BillingViewModel(get(), get(), get()) }
}

private val databaseModule = module {
    single { NewsDatabaseHelper(androidContext()) }
    single { buildLocalBillingDatabase(androidContext()) }
}

private val systemServiceModule = module {
    single { androidContext().getSystemService<NotificationManager>() }
}

private val miscellaneousSingletonModule = module {
    /**
     * A singleton [SystemVersionProperties] helps avoid unnecessary calls to the native `getprop` command.
     */
    single { SystemVersionProperties() }

    single { AppUpdateManagerFactory.create(androidContext()) }
    single { Firebase.analytics }
    single { Firebase.crashlytics }
    single { WorkManager.getInstance(androidContext()) }
}

val allModules = listOf(
    retrofitModule,
    networkModule,
    preferencesModule,
    repositoryModule,
    viewModelModule,
    databaseModule,
    systemServiceModule,
    miscellaneousSingletonModule
)
