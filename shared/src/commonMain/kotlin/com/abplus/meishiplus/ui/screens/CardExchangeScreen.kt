package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.ui.components.CameraPermissionGate
import com.abplus.meishiplus.ui.components.CardItem
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.ic_home
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerUiOptions
import org.ncgroup.kscan.ScannerView
import org.ncgroup.kscan.scannerColors
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardExchangeScreen(
    cardEntity: CardEntity,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    val tabs = listOf("QRコード", "QR読み取り")
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("名刺交換") },
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
            CardItem(
                cardEntity = cardEntity,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .aspectRatio(91f / 55f),
            )
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.padding(top = 16.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    when (page) {
                        0 -> CardIdQrCode(
                            cardId = cardEntity.id,
                            modifier = Modifier.size(160.dp).align(Alignment.Center),
                        )
                        else -> QrScannerPage(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardIdQrCode(
    cardId: String,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberQrCodePainter(cardId.ifBlank { " " }),
        contentDescription = "カードIDのQRコード",
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun QrScannerPage(
    modifier: Modifier = Modifier,
) {
    var scannedCardId by rememberSaveable { mutableStateOf<String?>(null) }

    CameraPermissionGate(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val scannerSize = minOf(
                maxWidth,
                (maxHeight - 24.dp).coerceAtLeast(0.dp),
                420.dp,
            )

            ScannerView(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(scannerSize)
                    .clipToBounds(),
                codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE),
                colors = scannerColors(
                    headerContainerColor = Color(0xFF00AFAF),
                    zoomControllerContainerColor = Color(0xFF00AFAF),
                    barcodeFrameColor = Color(0xFF00AFAF),
                ),
                scannerUiOptions = ScannerUiOptions(
                    headerTitle = "QR読み取り",
                    showZoom = true,
                    showTorch = true,
                ),
                result = { result ->
                    if (result is BarcodeResult.OnSuccess) {
                        scannedCardId = result.barcode.data
                    }
                },
            )
            scannedCardId?.let { cardId ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                    ),
                ) {
                    Text(
                        text = cardId,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}
