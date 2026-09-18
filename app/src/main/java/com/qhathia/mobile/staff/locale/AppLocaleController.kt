package com.qhathia.mobile.staff.locale

import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the app locale in step with the device-owned language preference.
 *
 * The preference in DataStore is the source of truth; AppCompat per-app locales apply it (the system
 * `LocaleManager` on API 33+, AppCompat's own storage below). On start:
 * - with no stored preference, the system language decides (English → English, anything else →
 *   Spanish) and is stored, so existing installs keep seeing Spanish;
 * - on API 33+, a language picked in the system's per-app settings is adopted into the preference.
 *
 * From then on every change of the preference — made in Configuración — is applied, which recreates
 * the activity in the new language.
 */
@Singleton
class AppLocaleController @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start() {
        scope.launch {
            reconcile()
            appPreferencesRepository.observeLanguage()
                .filterNotNull()
                .distinctUntilChanged()
                .collect(::apply)
        }
    }

    private suspend fun reconcile() {
        val stored = appPreferencesRepository.observeLanguage().first()
        val applied = AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())
        when {
            stored == null -> appPreferencesRepository.setLanguage(applied ?: systemLanguage())
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && applied != null && applied != stored ->
                appPreferencesRepository.setLanguage(applied)
        }
    }

    private fun apply(language: AppLanguage) {
        val target = LocaleListCompat.forLanguageTags(language.tag)
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != target.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(target)
        }
    }

    private fun systemLanguage(): AppLanguage =
        if (AppLanguage.fromTag(Resources.getSystem().configuration.locales[0]?.toLanguageTag()) == AppLanguage.ENGLISH) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.SPANISH
        }
}
