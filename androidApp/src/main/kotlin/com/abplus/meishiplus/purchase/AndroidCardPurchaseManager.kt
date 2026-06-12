package com.abplus.meishiplus.purchase

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidCardPurchaseManager(
    context: Context,
) : PurchasesUpdatedListener {
    sealed class PurchaseResult {
        data object Success : PurchaseResult()
        data class Failure(val message: String) : PurchaseResult()
    }

    private companion object {
        const val TAG = "AndroidCardPurchase"
        const val CARD_ADD_100YEN_PRODUCT_ID = "card_add_100yen"
    }

    private val appContext = context.applicationContext
    private var billingClient: BillingClient? = null
    private var pendingResult: kotlin.coroutines.Continuation<PurchaseResult>? = null

    suspend fun purchase(activity: Activity): PurchaseResult = suspendCancellableCoroutine { continuation ->
        pendingResult = continuation
        val client = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .enableAutoServiceReconnection()
            .build()
        billingClient = client

        continuation.invokeOnCancellation {
            finish(PurchaseResult.Failure("購入処理がキャンセルされました。"))
            client.endConnection()
        }

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    fail(client, "Google Play Billing の初期化に失敗しました: ${billingResult.responseCode} / ${billingResult.debugMessage}")
                    return
                }

                val product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(CARD_ADD_100YEN_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(listOf(product))
                    .build()

                client.queryProductDetailsAsync(params) { result, queryResult ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        fail(client, "商品情報の取得に失敗しました: ${result.responseCode} / ${result.debugMessage}")
                        return@queryProductDetailsAsync
                    }

                    val productDetails = queryResult.productDetailsList
                        .firstOrNull { it.productId == CARD_ADD_100YEN_PRODUCT_ID }
                        ?: run {
                            fail(client, "購入しようとしたアイテムが見つかりませんでした。")
                            return@queryProductDetailsAsync
                        }

                    val offerToken = productDetails.oneTimePurchaseOfferDetails?.offerToken

                    val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .also { if (offerToken != null) it.setOfferToken(offerToken) }
                        .build()

                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(productDetailsParams))
                        .build()

                    val launchResult = client.launchBillingFlow(activity, flowParams)
                    if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        fail(client, "購入画面を開けませんでした: ${launchResult.responseCode} / ${launchResult.debugMessage}")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Reconnect is handled by enableAutoServiceReconnection.
            }
        })
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            fail(billingClient, "購入更新に失敗しました: ${billingResult.responseCode} / ${billingResult.debugMessage}")
            return
        }

        val purchase = purchases?.firstOrNull {
            it.products.any { productId -> productId == CARD_ADD_100YEN_PRODUCT_ID }
        } ?: run {
            fail(billingClient, "購入結果に対象商品が含まれていませんでした。")
            return
        }

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            fail(billingClient, "購入状態が完了ではありませんでした: ${purchase.purchaseState}")
            return
        }

        if (purchase.isAcknowledged) {
            finish(PurchaseResult.Success)
            billingClient?.endConnection()
            return
        }

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient?.consumeAsync(consumeParams) { consumeResult, _ ->
            if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                finish(PurchaseResult.Success)
                billingClient?.endConnection()
            } else {
                fail(billingClient, "購入の消費に失敗しました: ${consumeResult.responseCode} / ${consumeResult.debugMessage}")
            }
        }
    }

    private fun fail(client: BillingClient?, message: String) {
        Log.e(TAG, message)
        finish(PurchaseResult.Failure(message))
        client?.endConnection()
    }

    private fun finish(result: PurchaseResult) {
        pendingResult?.resume(result)
        pendingResult = null
    }
}
