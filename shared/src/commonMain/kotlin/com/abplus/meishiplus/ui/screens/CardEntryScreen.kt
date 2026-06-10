package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.model.Account
import com.abplus.meishiplus.ui.components.CardEntry
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.ic_home
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEntryScreen(
    cardEntity: CardEntity,
    authenticatedAccounts: List<Account> = emptyList(),
    onCardChange: (CardEntity) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("名刺編集") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00AFAF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_home),
                            contentDescription = "戻る",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        CardEntry(
            cardEntity = cardEntity,
            onCardChange = onCardChange,
            authenticatedAccounts = authenticatedAccounts,
            onAuthenticatedAccountCheckedChange = { account, checked ->
                val updatedAccounts = if (checked) {
                    cardEntity.accounts
                        .filterNot { it.service.equals(account.service, ignoreCase = true) } + account
                } else {
                    cardEntity.accounts.filterNot {
                        it.service.equals(account.service, ignoreCase = true)
                    }
                }
                onCardChange(cardEntity.copy(accounts = updatedAccounts))
            },
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
