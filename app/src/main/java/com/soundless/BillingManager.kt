package com.soundless

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "SoundlessBilling"
const val PRODUCT_ID = "remove_ads"

class BillingManager(context: Context) : PurchasesUpdatedListener {

    private val prefs = context.getSharedPreferences("soundless", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _adsRemoved = MutableStateFlow(prefs.getBoolean("ads_removed", false))
    val adsRemoved: StateFlow<Boolean> = _adsRemoved

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing disconnected")
            }
        })
    }

    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPurchase = purchases.any {
                    it.products.contains(PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasPurchase) {
                    setAdsRemoved(true)
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        scope.launch {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val result = billingClient.queryProductDetails(params)
            val detailsList = result.productDetailsList
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK
                && detailsList != null && detailsList.isNotEmpty()
            ) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(detailsList[0])
                                .build()
                        )
                    )
                    .build()
                activity.runOnUiThread {
                    billingClient.launchBillingFlow(activity, flowParams)
                }
            } else {
                Log.e(TAG, "Product query failed: ${result.billingResult.debugMessage}")
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            setAdsRemoved(true)
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { r ->
                    Log.d(TAG, "Acknowledge: ${r.responseCode}")
                }
            }
        }
    }

    private fun setAdsRemoved(removed: Boolean) {
        _adsRemoved.value = removed
        prefs.edit().putBoolean("ads_removed", removed).apply()
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
