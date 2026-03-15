package id.stargan.intikasirfnb.licensing

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.stargan.intikasirfnb.BuildConfig
import id.stargan.intikasirfnb.feature.identity.ui.splash.LicenseChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LicenseModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // Certificate pinning — aktif hanya pada release build
        if (!BuildConfig.DEBUG && AppConfig.CERTIFICATE_PINS.isNotEmpty() && AppConfig.CERT_PIN_HOSTNAME.isNotBlank()) {
            val pinner = CertificatePinner.Builder().apply {
                AppConfig.CERTIFICATE_PINS.forEach { pin ->
                    add(AppConfig.CERT_PIN_HOSTNAME, pin)
                }
            }.build()
            builder.certificatePinner(pinner)
        }

        // Logging hanya pada debug
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideAppRegApi(client: OkHttpClient): AppRegApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.APPREG_BASE_URL + "/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AppRegApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDeviceIdProvider(@ApplicationContext context: Context): DeviceIdProvider {
        return DeviceIdProvider(context)
    }

    @Provides
    @Singleton
    fun providePlayIntegrityHelper(@ApplicationContext context: Context): PlayIntegrityHelper {
        return PlayIntegrityHelper(context)
    }

    @Provides
    @Singleton
    fun provideLicenseStorage(@ApplicationContext context: Context): LicenseStorage {
        return LicenseStorage(context)
    }

    @Provides
    @Singleton
    fun provideLicenseVerifier(): LicenseVerifier {
        return LicenseVerifier
    }

    @Provides
    @Singleton
    fun provideActivationRepository(
        api: AppRegApi,
        integrityHelper: PlayIntegrityHelper,
        licenseStorage: LicenseStorage,
        deviceIdProvider: DeviceIdProvider,
    ): ActivationRepository {
        return ActivationRepository(api, integrityHelper, licenseStorage, deviceIdProvider)
    }

    @Provides
    @Singleton
    fun provideLicenseRevalidator(
        api: AppRegApi,
        licenseStorage: LicenseStorage,
        deviceIdProvider: DeviceIdProvider,
    ): LicenseRevalidator {
        return LicenseRevalidator(api, licenseStorage, deviceIdProvider)
    }

    @Provides
    @Singleton
    fun provideLicenseChecker(
        licenseStorage: LicenseStorage,
        deviceIdProvider: DeviceIdProvider,
        licenseRevalidator: LicenseRevalidator,
    ): LicenseChecker {
        return LicenseCheckerImpl(
            licenseStorage = licenseStorage,
            deviceIdProvider = deviceIdProvider,
            licenseRevalidator = licenseRevalidator,
            applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }
}
