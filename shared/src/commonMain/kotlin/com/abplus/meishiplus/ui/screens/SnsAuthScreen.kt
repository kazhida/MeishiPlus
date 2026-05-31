package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import meishiplus.shared.generated.resources.ic_home
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
    onBackClick: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val snsAccounts = listOf(
        SnsAccountItemSpec(
            serviceName = "GitHub",
            icon = Res.drawable.ic_sns_github,
            account = userEntity.accounts.firstOrNull { it is Account.Github || it.service == "github" },
        ),
        SnsAccountItemSpec(
            serviceName = "Facebook",
            icon = Res.drawable.ic_sns_facebook,
            account = userEntity.accounts.firstOrNull { it is Account.Facebook || it.service == "facebook" },
        ),
        SnsAccountItemSpec(
            serviceName = "Instagram",
            icon = Res.drawable.ic_sns_instagram,
            account = userEntity.accounts.firstOrNull { it is Account.Instagram || it.service == "instagram" },
        ),
        SnsAccountItemSpec(
            serviceName = "X",
            icon = Res.drawable.ic_sns_x,
            account = userEntity.accounts.firstOrNull { it is Account.X || it.service == "x" || it.service == "twitter" },
        ),
        SnsAccountItemSpec(
            serviceName = "Qiita",
            icon = Res.drawable.ic_sns_qiita,
            account = userEntity.accounts.firstOrNull { it is Account.Qiita || it.service == "qiita" },
        ),
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(vertical = 8.dp),
        ) {
            items(
                items = snsAccounts,
                key = { it.serviceName },
            ) { item ->
                SnsAuthListItem(
                    item = item,
                    onClick = when (item.serviceName) {
                        "GitHub" -> {
                            {
                                coroutineScope.launch {
                                    uriHandler.openUri(GithubAuth.authorizationUrl())
                                }
                            }
                        }
                        "Facebook" -> {
                            {
                                coroutineScope.launch {
                                    uriHandler.openUri(FacebookAuth.authorizationUrl())
                                }
                            }
                        }
                        "Instagram" -> {
                            {
                                coroutineScope.launch {
                                    uriHandler.openUri(InstagramAuth.authorizationUrl())
                                }
                            }
                        }
                        "Qiita" -> {
                            {
                                coroutineScope.launch {
                                    uriHandler.openUri(QiitaAuth.authorizationUrl())
                                }
                            }
                        }
                        "X" -> {
                            {
                                coroutineScope.launch {
                                    uriHandler.openUri(XAuth.authorizationUrl())
                                }
                            }
                        }
                        else -> null
                    },
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
}

@Composable
private fun SnsAuthListItem(
    item: SnsAccountItemSpec,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(
                enabled = onClick != null,
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
            Text(
                text = item.account?.userId ?: "未認証",
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
    }
}

@Immutable
private data class SnsAccountItemSpec(
    val serviceName: String,
    val icon: DrawableResource,
    val account: Account?,
)
