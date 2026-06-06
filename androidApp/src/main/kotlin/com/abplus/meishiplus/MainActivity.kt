package com.abplus.meishiplus

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.abplus.meishiplus.auth.AndroidAuthGate
import com.abplus.meishiplus.data.repositories.firestore.FireStoreCardRepository
import com.abplus.meishiplus.data.repositories.firestore.FireStoreUserRepository
import com.abplus.meishiplus.data.usecase.UserInit
import com.abplus.meishiplus.pdf.AndroidCardPdfContext
import com.abplus.meishiplus.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by lazy {
        val firestore = FirebaseFirestore.getInstance()
        val userRepository = FireStoreUserRepository(firestore)
        val cardRepository = FireStoreCardRepository(firestore)
        UserViewModel(
            userInit = UserInit(userRepository, cardRepository),
            cardRepository = cardRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        AndroidCardPdfContext.applicationContext = applicationContext

        setContent {
            AndroidAuthGate(userViewModel = userViewModel)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
