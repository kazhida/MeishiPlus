package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.abplus.meishiplus.data.repositories.CardRepository
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
    cardRepository: CardRepository? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onEditCard: (Int) -> Unit = {},
    onLayoutCard: (Int) -> Unit = {},
    onPrintCard: (Int) -> Unit = {},
    onExchangeCard: (Int) -> Unit = {},
    onPreviewCard: (Int) -> Unit = {},
    onPreviewPartnerCard: (CardEntity) -> Unit = {},
    onSnsAuthClick: () -> Unit = {},
) {
    val isInteractionEnabled = !isRefreshing
    val cards = appUser?.cards.orEmpty()
    val tabs = if (cards.isNotEmpty()) {
        cards.mapIndexed { index, card -> card.caption.ifBlank { "名刺${index + 1}" } }
    } else {
        listOf("------")
    }
    val drawerItems = listOf(
        DrawerItem("ホーム", Res.drawable.ic_home, DrawerDestination.Home),
        DrawerItem("SNS認証", Res.drawable.ic_badge, DrawerDestination.SnsAuth),
        DrawerItem("設定", Res.drawable.ic_settings, DrawerDestination.Settings),
    )
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isInteractionEnabled,
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
                        selected = item.destination == DrawerDestination.Home,
                        onClick = {
                            if (!isInteractionEnabled) return@NavigationDrawerItem
                            when (item.destination) {
                                DrawerDestination.SnsAuth -> onSnsAuthClick()
                                DrawerDestination.Home,
                                DrawerDestination.Settings -> {
                                    coroutineScope.launch {
                                        drawerState.close()
                                    }
                                }
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
                        onClick = {
                            if (isInteractionEnabled) {
                                onSignOut()
                            }
                        },
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
                                enabled = isInteractionEnabled,
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
                                enabled = isInteractionEnabled,
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
                        userScrollEnabled = isInteractionEnabled,
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
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    TabPage(
                                        title = tabs[page],
                                        cardIndex = page,
                                        cardEntity = cards.getOrNull(page),
                                        cards = cards,
                                        cardRepository = cardRepository,
                                        onPreviewCard = onPreviewCard,
                                        onPreviewPartnerCard = onPreviewPartnerCard,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                    ) {
                                        CardItemActionMenu(
                                            enabled = isInteractionEnabled,
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
}

private data class DrawerItem(
    val title: String,
    val icon: DrawableResource,
    val destination: DrawerDestination,
)

private enum class DrawerDestination {
    Home,
    SnsAuth,
    Settings,
}

@Composable
private fun TabPage(
    title: String,
    cardIndex: Int,
    cardEntity: CardEntity?,
    cards: List<CardEntity>,
    cardRepository: CardRepository?,
    onPreviewCard: (Int) -> Unit,
    onPreviewPartnerCard: (CardEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = cardEntity ?: CardEntity.default().copy(
        id = title.hashCode().toString(),
        name = CardEntity.default().name.copy(value = title),
    )
    var partnerCards by remember(card.id, card.partnerIds, cardRepository) {
        mutableStateOf<List<Pair<String, CardEntity?>>>(emptyList())
    }
    var isPartnerCardsLoading by remember(card.id, card.partnerIds, cardRepository) {
        mutableStateOf(card.partnerIds.isNotEmpty() && cardRepository != null)
    }
    LaunchedEffect(card.id, card.partnerIds, cardRepository) {
        val repository = cardRepository
        if (repository == null || card.partnerIds.isEmpty()) {
            partnerCards = emptyList()
            isPartnerCardsLoading = false
        } else {
            isPartnerCardsLoading = true
            try {
                partnerCards = card.partnerIds.map { partnerId ->
                    partnerId to runCatching {
                        repository.getCard(partnerId)
                    }.getOrNull()
                }
            } finally {
                isPartnerCardsLoading = false
            }
        }
    }

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
        when {
            isPartnerCardsLoading -> {
                PartnerCardsLoadingSection(
                    partnerCount = card.partnerIds.size,
                )
            }
            partnerCards.isNotEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    partnerCards.chunked(2).forEach { rowCards ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowCards.forEach { (_, partnerCard) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(91f / 55f),
                                ) {
                                    if (partnerCard != null) {
                                        CardItem(
                                            cardEntity = partnerCard,
                                            modifier = Modifier.fillMaxSize(),
                                            fontScale = 0.5f,
                                            onCardClick = {
                                                onPreviewPartnerCard(partnerCard)
                                            },
                                        )
                                    } else {
                                        PartnerCardPlaceholder(
                                            text = "未共有",
                                            showProgress = false,
                                        )
                                    }
                                }
                            }
                            if (rowCards.size == 1) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(91f / 55f),
                                )
                            }
                        }
                    }
                }
            }
            card.partnerIds.isNotEmpty() -> {
                PartnerCardsLoadingSection(
                    partnerCount = card.partnerIds.size,
                    message = "未共有",
                )
            }
        }
    }
}

@Composable
private fun PartnerCardsLoadingSection(
    partnerCount: Int,
    message: String = "読み込み中",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat((partnerCount + 1) / 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(2) { columnIndex ->
                    if (it * 2 + columnIndex < partnerCount) {
                        PartnerCardPlaceholder(
                            text = message,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(91f / 55f),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(91f / 55f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerCardPlaceholder(
    text: String,
    showProgress: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CardItemActionMenu(
    enabled: Boolean,
    onMenuClick: () -> Unit,
    onEditClick: () -> Unit,
    onLayoutClick: () -> Unit,
    onPrintClick: () -> Unit,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isActionMenuVisible by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            isActionMenuVisible = false
        }
    }

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
            enabled = enabled,
            onClick = {
                isActionMenuVisible = !isActionMenuVisible
                onMenuClick()
            },
        )
        if (isActionMenuVisible) {
            CardItemActionButton(
                icon = Res.drawable.ic_edit,
                contentDescription = "編集",
                enabled = enabled,
                onClick = onEditClick,
            )
            CardItemActionButton(
                icon = Res.drawable.ic_layout,
                contentDescription = "レイアウト",
                enabled = enabled,
                onClick = onLayoutClick,
            )
            CardItemActionButton(
                icon = Res.drawable.ic_print,
                contentDescription = "印刷",
                enabled = enabled,
                onClick = onPrintClick,
            )
            CardItemActionButton(
                icon = Res.drawable.ic_swap,
                contentDescription = "交換",
                enabled = enabled,
                onClick = onSwapClick,
            )
        }
    }
}

@Composable
private fun CardItemActionButton(
    icon: DrawableResource,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        enabled = enabled,
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
