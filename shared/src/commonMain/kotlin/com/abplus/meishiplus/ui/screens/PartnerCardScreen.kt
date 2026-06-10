package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.model.Account
import com.abplus.meishiplus.ui.components.CardItem
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.ic_home
import meishiplus.shared.generated.resources.ic_sns_facebook
import meishiplus.shared.generated.resources.ic_sns_github
import meishiplus.shared.generated.resources.ic_sns_google
import meishiplus.shared.generated.resources.ic_sns_instagram
import meishiplus.shared.generated.resources.ic_sns_qiita
import meishiplus.shared.generated.resources.ic_sns_x
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerCardScreen(
    cardEntity: CardEntity,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    val accountItems = cardEntity.accounts.map { it.toPartnerAccountItemSpec() }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("共有名刺") },
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .aspectRatio(91f / 55f),
            ) {
                CardItem(
                    cardEntity = cardEntity,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = accountItems,
                    key = { index, item -> "${index}:${item.serviceName}:${item.account.userName}" },
                ) { index, item ->
                    val onItemClick = item.account.userUrl.takeIf { it.isNotBlank() }?.let { url ->
                        { uriHandler.openUri(url) }
                    }
                    PartnerAccountListItem(
                        item = item,
                        onClick = onItemClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (index < accountItems.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
                if (cardEntity.remark.isNotBlank()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = cardEntity.remark,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerAccountListItem(
    item: PartnerAccountItemSpec,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(enabled = onClick != null, onClick = { onClick?.invoke() }),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(item.icon),
                contentDescription = item.serviceName,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.account.displayLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Immutable
private data class PartnerAccountItemSpec(
    val serviceName: String,
    val icon: DrawableResource,
    val account: Account,
)

private fun Account.toPartnerAccountItemSpec(): PartnerAccountItemSpec {
    return PartnerAccountItemSpec(
        serviceName = serviceNameLabel(),
        icon = serviceIcon(),
        account = this,
    )
}

private fun Account.serviceNameLabel(): String {
    return when (this) {
        is Account.Github -> "GitHub"
        is Account.X -> "X"
        is Account.Qiita -> "Qiita"
        is Account.Facebook -> "Facebook"
        is Account.Instagram -> "Instagram"
        is Account.Google -> "Google"
    }
}

private fun Account.serviceIcon(): DrawableResource {
    return when (this) {
        is Account.Github -> Res.drawable.ic_sns_github
        is Account.X -> Res.drawable.ic_sns_x
        is Account.Qiita -> Res.drawable.ic_sns_qiita
        is Account.Facebook -> Res.drawable.ic_sns_facebook
        is Account.Instagram -> Res.drawable.ic_sns_instagram
        is Account.Google -> Res.drawable.ic_sns_google
    }
}

private fun Account.displayLabel(): String {
    return when (this) {
        is Account.X -> displayName?.takeIf { it.isNotBlank() } ?: userName
        else -> userName
    }
}
