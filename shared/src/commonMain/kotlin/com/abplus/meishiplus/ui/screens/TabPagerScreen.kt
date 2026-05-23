package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.data.repositories.LocalAppRepositories
import com.abplus.meishiplus.ui.components.CardEntry
import com.abplus.meishiplus.ui.components.ProfileHeader
import kotlinx.coroutines.launch
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.ic_badge
import meishiplus.shared.generated.resources.ic_home
import meishiplus.shared.generated.resources.ic_logout
import meishiplus.shared.generated.resources.ic_menu
import meishiplus.shared.generated.resources.ic_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabPagerScreen(
    authUser: AuthUser? = null,
    onSignOut: (() -> Unit)? = null,
) {
    val repositories = LocalAppRepositories.current
    var user by remember(authUser?.uid) { mutableStateOf<AppUser?>(null) }
    var loadErrorMessage by remember(authUser?.uid) { mutableStateOf<String?>(null) }
    LaunchedEffect(authUser?.uid, repositories) {
        val uid = authUser?.uid
        if (uid == null || repositories == null) {
            user = null
            loadErrorMessage = null
            return@LaunchedEffect
        }

        runCatching {
            loadUser(uid = uid, repositories = repositories)
        }.onSuccess { loadedUser ->
            user = loadedUser
            loadErrorMessage = null
        }.onFailure { throwable ->
            user = null
            loadErrorMessage = throwable.message ?: "ユーザー情報を取得できませんでした。"
        }
    }

    val cards = user?.cards.orEmpty()
    val tabs = if (cards.isNotEmpty()) {
        cards.mapIndexed { index, card -> card.name.ifBlank { "名刺${index + 1}" } }
    } else {
        listOf("基本", "詳細", "設定", "履歴", "その他")
    }
    val drawerItems = listOf(
        DrawerItem("ホーム", Res.drawable.ic_home),
        DrawerItem("名刺", Res.drawable.ic_badge),
        DrawerItem("設定", Res.drawable.ic_settings),
    )
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f),
            ) {
                ProfileHeader(authUser = authUser)
                drawerItems.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(item.icon),
                                contentDescription = null,
                            )
                        },
                        label = { Text(item.title) },
                        selected = index == 0,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                if (authUser != null && onSignOut != null) {
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_logout),
                                contentDescription = null,
                            )
                        },
                        label = { Text("ログアウト") },
                        selected = false,
                        onClick = onSignOut,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("名刺＋") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF00AFAF),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_menu),
                                contentDescription = "メニューを開く",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) },
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    if (loadErrorMessage != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = loadErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        TabPage(
                            title = tabs[page],
                            cardEntity = cards.getOrNull(page),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

private data class DrawerItem(
    val title: String,
    val icon: DrawableResource,
)

@Composable
private fun TabPage(
    title: String,
    cardEntity: CardEntity?,
    modifier: Modifier = Modifier,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    val card = cardEntity ?: CardEntity(
        id = title.hashCode().toString(),
        name = title,
        createdAt = now,
        updatedAt = now,
    )
    CardEntry(cardEntity = card, onCardChange = {})
}

private suspend fun loadUser(
    uid: String,
    repositories: com.abplus.meishiplus.data.repositories.AppRepositories,
): AppUser {
    val userEntity = runCatching {
        repositories.userRepository.getUser(uid)
    }.getOrElse {
        repositories.userRepository.addUser(UserEntity(id = uid))
    }
    val cards = if (userEntity.cardIds.isEmpty()) {
        emptyList()
    } else {
        repositories.cardRepository.getCards(userEntity.cardIds)
    }
    return AppUser(user = userEntity, cards = cards)
}
