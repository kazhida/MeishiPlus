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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.entities.CardEntity
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
    val tabs = listOf("基本", "詳細", "設定", "履歴", "その他")
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
                    TabPage(
                        title = tabs[page],
                        modifier = Modifier.fillMaxSize(),
                    )
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
    modifier: Modifier = Modifier,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    CardEntity(
        id = title.hashCode().toString(),
        name = title,
        createdAt = now,
        updatedAt = now,
    ).let { cardEntity ->
        CardEntry(cardEntity = cardEntity, onCardChange = {})
    }
}
