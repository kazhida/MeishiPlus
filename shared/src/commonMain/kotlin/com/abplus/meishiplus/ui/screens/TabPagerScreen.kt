package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.ui.components.CardItem
import com.abplus.meishiplus.ui.components.ProfileHeader
import kotlinx.coroutines.launch
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.bg
import meishiplus.shared.generated.resources.ic_badge
import meishiplus.shared.generated.resources.ic_edit
import meishiplus.shared.generated.resources.ic_home
import meishiplus.shared.generated.resources.ic_layout
import meishiplus.shared.generated.resources.ic_logout
import meishiplus.shared.generated.resources.ic_menu
import meishiplus.shared.generated.resources.ic_print
import meishiplus.shared.generated.resources.ic_settings
import meishiplus.shared.generated.resources.ic_swap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabPagerScreen(
    authUser: AuthUser? = null,
    appUser: AppUser? = null,
    errorMessage: String? = null,
    onSignOut: (() -> Unit)? = null,
    onEditCard: (Int) -> Unit = {},
    onLayoutCard: (Int) -> Unit = {},
    onPrintCard: (Int) -> Unit = {},
    onExchangeCard: (Int) -> Unit = {},
    onPreviewCard: (Int) -> Unit = {},
) {
    val cards = appUser?.cards.orEmpty()
    val tabs = if (cards.isNotEmpty()) {
        cards.mapIndexed { index, card -> card.caption.ifBlank { "名刺${index + 1}" } }
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
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Res.drawable.bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Scaffold(
                containerColor = Color.Transparent,
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
                        if (errorMessage != null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                TabPage(
                                    title = tabs[page],
                                    cardIndex = page,
                                    cardEntity = cards.getOrNull(page),
                                    onPreviewCard = onPreviewCard,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                ) {
                                    CardItemActionMenu(
                                        onMenuClick = {},
                                onEditClick = { onEditCard(page) },
                                onLayoutClick = { onLayoutCard(page) },
                                onPrintClick = { onPrintCard(page) },
                                onSwapClick = { onExchangeCard(page) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                            .widthIn(max = 460.dp)
                                            .wrapContentSize(Alignment.TopEnd),
                                    )
                                }
                            }
                        }
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
    cardIndex: Int,
    cardEntity: CardEntity?,
    onPreviewCard: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = cardEntity ?: CardEntity.default().copy(
        id = title.hashCode().toString(),
        name = CardEntity.default().name.copy(value = title),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .aspectRatio(91f / 55f),
        ) {
            CardItem(
                cardEntity = card,
                modifier = Modifier.fillMaxSize(),
                onCardClick = { onPreviewCard(cardIndex) },
            )
        }
    }
}

@Composable
private fun CardItemActionMenu(
    onMenuClick: () -> Unit,
    onEditClick: () -> Unit,
    onLayoutClick: () -> Unit,
    onPrintClick: () -> Unit,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isActionMenuVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(
            top = 8.dp,
            end = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardItemActionButton(
            icon = Res.drawable.ic_menu,
            contentDescription = "メニュー",
            onClick = {
                isActionMenuVisible = !isActionMenuVisible
                onMenuClick()
            },
        )
        if (isActionMenuVisible) {
            CardItemActionButton(
                icon = Res.drawable.ic_edit,
                contentDescription = "編集",
                onClick = onEditClick,
            )
            CardItemActionButton(
                icon = Res.drawable.ic_layout,
                contentDescription = "レイアウト",
                onClick = onLayoutClick,
            )
            CardItemActionButton(
                icon = Res.drawable.ic_print,
                contentDescription = "印刷",
                onClick = onPrintClick,
            )
            CardItemActionButton(
                icon = Res.drawable.ic_swap,
                contentDescription = "交換",
                onClick = onSwapClick,
            )
        }
    }
}

@Composable
private fun CardItemActionButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.25f)),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
