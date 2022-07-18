package com.bashkir.internetsurfer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.edit
import com.bashkir.internetsurfer.ui.theme.InternetSurferTheme
import com.google.accompanist.web.*
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.message
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firstPage = getPreferences(MODE_PRIVATE).getString("web", null)
        setContent {
            InternetSurferTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    WebViewScreen(firstPage)
                }
            }
        }
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(firstPage: String?) = Column(Modifier.fillMaxSize()) {
    val state = rememberWebViewState(firstPage ?: "https://yandex.ru/")
    val navigator = rememberWebViewNavigator()
    val warningDialogState = rememberMaterialDialogState()

    val loadingState = state.loadingState
    val activity = (LocalContext.current as? Activity)

    if (loadingState is LoadingState.Loading)
        LinearProgressIndicator(loadingState.progress, Modifier.fillMaxWidth())

    WebView(
        state,
        Modifier.fillMaxSize(),
        navigator = navigator,
        onCreated = {
            it.settings.javaScriptEnabled = true
            it.settings.cacheMode = WebSettings.LOAD_DEFAULT
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(it, true)
            }
        },
        client = object : AccompanistWebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val yandexMapsUrl = "https://yandex.ru/maps/"
                val yandexWeatherPackage = "ru.yandex.weatherplugin"
                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

                sharedPref?.edit(true) {
                    putString("web", url)
                }

                if (url?.contains(yandexMapsUrl) == true) {
                    val yandexMapIntent =
                        Intent(ACTION_VIEW, Uri.parse("yandexmaps://maps.yandex.ru/"))
                    view!!.context.startActivity(
                        if (yandexMapIntent.resolveActivity(view.context.packageManager) != null)
                            yandexMapIntent
                        else Intent(ACTION_VIEW, Uri.parse(yandexMapsUrl))
                    )
                }

                if (url?.contains("https://yandex.ru/pogoda/") == true) {
                    val weatherIntent =
                        view!!.context.packageManager.getLaunchIntentForPackage(yandexWeatherPackage)
                    if (weatherIntent != null)
                        view.context.startActivity(weatherIntent)
                }

                super.onPageStarted(view, url, favicon)
            }
        }
    )

    MaterialDialog(warningDialogState, buttons = {
        positiveButton("Выход") { activity?.finish() }
        negativeButton("Отмена")
    }) {
        title("Вы уверены, что хотите закрыть приложение?")
    }

    BackHandler(!navigator.canGoBack) {
        warningDialogState.show()
    }
}