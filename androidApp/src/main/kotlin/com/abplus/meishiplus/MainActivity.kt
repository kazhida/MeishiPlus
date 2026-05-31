package com.abplus.meishiplus

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.abplus.meishiplus.auth.AndroidAuthGate
import com.abplus.meishiplus.pdf.AndroidCardPdfContext
import com.abplus.meishiplus.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userViewModel: UserViewModel

    private val deepLinkUri = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        AndroidCardPdfContext.applicationContext = applicationContext
        handleIntent(intent)

        setContent {
            AndroidAuthGate(
                userViewModel = userViewModel,
                deepLinkUri = deepLinkUri,
                onDeepLinkConsumed = {
                    deepLinkUri.value = null
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        deepLinkUri.value = intent?.data
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
