package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.auth.FacebookAuth
import com.abplus.meishiplus.auth.GithubAuth
import com.abplus.meishiplus.auth.InstagramAuth
import com.abplus.meishiplus.auth.QiitaAuth
import com.abplus.meishiplus.auth.XAuth
import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.model.Account
import kotlinx.coroutines.launch
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.ic_back
import meishiplus.shared.generated.resources.ic_sns_facebook
import meishiplus.shared.generated.resources.ic_sns_github
import meishiplus.shared.generated.resources.ic_sns_instagram
import meishiplus.shared.generated.resources.ic_sns_qiita
import meishiplus.shared.generated.resources.ic_sns_x
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnsAuthScreen(
    userEntity: UserEntity = UserEntity(),
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onUnlinkAccount: suspend (Account) -> Unit = {},
) {
    val isInteractionEnabled = !isRefreshing
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val snsAccounts = listOf(
        SnsAccountItemSpec(
            serviceName = "GitHub",
            icon = Res.drawable.ic_sns_github,
            account = userEntity.accounts.firstOrNull { it is Account.Github || it.service == "github" },
        ),
        SnsAccountItemSpec(
            serviceName = "X",
            icon = Res.drawable.ic_sns_x,
            account = userEntity.accounts.firstOrNull { it is Account.X || it.service == "x" },
        ),
        SnsAccountItemSpec(
            serviceName = "Qiita",
            icon = Res.drawable.ic_sns_qiita,
            account = userEntity.accounts.firstOrNull { it is Account.Qiita || it.service == "qiita" },
        ),
        SnsAccountItemSpec(
            serviceName = "Facebook",
            icon = Res.drawable.ic_sns_facebook,
            account = userEntity.accounts.firstOrNull { it is Account.Facebook || it.service == "facebook" },
        ),
//        SnsAccountItemSpec(
//            serviceName = "Instagram",
//            icon = Res.drawable.ic_sns_instagram,
//            account = userEntity.accounts.firstOrNull { it is Account.Instagram || it.service == "instagram" },
//        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SNS認証") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00AFAF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
                navigationIcon = {
                    IconButton(
                        enabled = isInteractionEnabled,
                        onClick = onBackClick,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back),
                            contentDescription = "戻る",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                ) {
                    items(
                        items = snsAccounts,
                        key = { it.serviceName },
                    ) { item ->
                        val onItemClick = if (isInteractionEnabled) {
                            item.account?.profileUrl
                                ?.takeIf { it.isNotBlank() }
                                ?.let { userUrl ->
                                    {
                                        coroutineScope.launch {
                                            uriHandler.openUri(userUrl)
                                        }
                                        Unit
                                    }
                                }
                                ?: when (item.serviceName) {
                                    "GitHub" -> {
                                        {
                                            coroutineScope.launch {
                                                uriHandler.openUri(GithubAuth.authorizationUrl())
                                            }
                                            Unit
                                        }
                                    }
                                    "Facebook" -> {
                                        {
                                            coroutineScope.launch {
                                                val url = FacebookAuth.authorizationUrl()
                                                println("DEBUG Facebook authorizationUrl: $url")
                                                uriHandler.openUri(url)
                                            }
                                            Unit
                                        }
                                    }
                                    "Instagram" -> {
                                        {
                                            coroutineScope.launch {
                                                uriHandler.openUri(InstagramAuth.authorizationUrl())
                                            }
                                            Unit
                                        }
                                    }
                                    "Qiita" -> {
                                        {
                                            coroutineScope.launch {
                                                uriHandler.openUri(QiitaAuth.authorizationUrl())
                                            }
                                            Unit
                                        }
                                    }
                                    "X" -> {
                                        {
                                            coroutineScope.launch {
                                                uriHandler.openUri(XAuth.authorizationUrl())
                                            }
                                            Unit
                                        }
                                    }
                                    else -> null
                                }
                        } else {
                            null
                        }
                        SnsAuthListItem(
                            item = item,
                            onClick = onItemClick,
                            onUnlinkClick = if (isInteractionEnabled && item.account != null) {
                                {
                                    val account = requireNotNull(item.account)
                                    coroutineScope.launch {
                                        onUnlinkAccount(account)
                                    }
                                }
                            } else {
                                null
                            },
                            enabled = isInteractionEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
            if (isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun SnsAuthListItem(
    item: SnsAccountItemSpec,
    onClick: (() -> Unit)?,
    onUnlinkClick: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled && onClick != null,
                onClick = { onClick?.invoke() },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.icon),
            contentDescription = item.serviceName,
            modifier = Modifier.size(40.dp),
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
            val accountName = when(item.account) {
                is Account.X -> item.account.displayName
                else -> item.account?.userName
            }
            Text(
                text = accountName ?: "未認証",
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.account == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.account != null) {
            TextButton(
                enabled = enabled && onUnlinkClick != null,
                onClick = { onUnlinkClick?.invoke() },
            ) {
                Text("解除")
            }
        }
    }
}

@Immutable
private data class SnsAccountItemSpec(
    val serviceName: String,
    val icon: DrawableResource,
    val account: Account?,
)
