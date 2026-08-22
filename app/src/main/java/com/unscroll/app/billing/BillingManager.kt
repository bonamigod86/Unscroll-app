package com.unscroll.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.QueryProductDetailsParams
import com.unscroll.app.data.PrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PurchaseStatus { IDLE, PENDING, FAILED }

class BillingManager(context: Context) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val prefs = PrefsRepository.get(appContext)

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _status = MutableStateFlow(PurchaseStatus.IDLE)
    val status: StateFlow<PurchaseStatus> = _status

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
        .build()

    fun connect() {
        if (_connected.value) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connected.value = true
                    queryProducts()
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                _connected.value = false
            }
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = detailsList.filterNotNull()
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = _products.value.firstOrNull() ?: return
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        _status.value = PurchaseStatus.IDLE
        client.launchBillingFlow(activity, params)
    }

    fun weeklyPriceFallback(default: String): String {
        val details = _products.value.firstOrNull() ?: return default
        val phases = details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?: return default
        val paid = phases.firstOrNull { it.priceAmountMicros > 0 } ?: return default
        val suffix = when (paid.billingPeriod) {
            "P1W" -> "/ week"
            "P1M" -> "/ month"
            "P1Y" -> "/ year"
            else -> ""
        }
        return "${paid.formattedPrice} $suffix".trim()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> _status.value = PurchaseStatus.IDLE
            else -> _status.value = PurchaseStatus.FAILED
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                _status.value = PurchaseStatus.PENDING
            }
            return
        }
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { }
        }
        if (purchase.products.contains(PRODUCT_ID)) {
            prefs.setSubscribed(true)
            _status.value = PurchaseStatus.IDLE
        }
    }

    fun restorePurchases() {
        if (!_connected.value) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val activeSub = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    it.products.contains(PRODUCT_ID)
            }
            if (activeSub) prefs.setSubscribed(true)
        }
    }

    fun dispose() {
        client.endConnection()
    }

    companion object {
        const val PRODUCT_ID = "unscroll_weekly"
    }
}
