package com.example.util

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AppLanguage
import java.util.Locale

/**
 * Resolves Android string resources (`res/values-ar`, `res/values-fr`, `res/values`)
 * dynamically according to the active [AppLanguage] (`AR` primary, `FR` secondary, `EN` fallback).
 */
object LocalizedResourceHelper {

  fun localeForLanguage(language: AppLanguage): Locale =
    when (language) {
      AppLanguage.AR -> Locale.forLanguageTag("ar")
      AppLanguage.FR -> Locale.FRENCH
      AppLanguage.EN -> Locale.ENGLISH
    }

  fun createLocalizedContext(baseContext: Context, language: AppLanguage): Context {
    val targetLocale = localeForLanguage(language)
    val config = Configuration(baseContext.resources.configuration)
    config.setLocale(targetLocale)
    config.setLayoutDirection(targetLocale)
    return baseContext.createConfigurationContext(config)
  }
}

@Composable
fun rememberLocalizedContext(language: AppLanguage): Context {
  val baseContext = LocalContext.current
  return remember(baseContext, language) {
    LocalizedResourceHelper.createLocalizedContext(baseContext, language)
  }
}

@Composable
fun trStringResource(
  language: AppLanguage,
  @StringRes id: Int,
  vararg formatArgs: Any,
): String {
  val localizedContext = rememberLocalizedContext(language)
  return if (formatArgs.isEmpty()) {
    localizedContext.resources.getString(id)
  } else {
    localizedContext.resources.getString(id, *formatArgs)
  }
}
