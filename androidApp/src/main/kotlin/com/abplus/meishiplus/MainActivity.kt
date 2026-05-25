package com.abplus.meishiplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.abplus.meishiplus.auth.AndroidAuthGate
import com.abplus.meishiplus.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
