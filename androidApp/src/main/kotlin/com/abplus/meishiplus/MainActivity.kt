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
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.data.repositories.firestore.FireStoreCardRepository
import com.abplus.meishiplus.data.repositories.firestore.FireStoreUserRepository
import com.abplus.meishiplus.data.usecase.UserInit
import com.abplus.meishiplus.pdf.AndroidCardPdfContext
import com.abplus.meishiplus.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val userRepository: UserRepository by lazy { FireStoreUserRepository(firestore) }
    private val cardRepository: CardRepository by lazy { FireStoreCardRepository(firestore) }
    private val userViewModel: UserViewModel by lazy {
        UserViewModel(
            userInit = UserInit(userRepository, cardRepository),
            cardRepository = cardRepository,
        )
    }

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
                userRepository = userRepository,
                cardRepository = cardRepository,
                deepLinkUri = deepLinkUri,
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
