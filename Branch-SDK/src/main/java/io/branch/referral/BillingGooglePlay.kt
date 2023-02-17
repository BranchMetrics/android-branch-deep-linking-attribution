package io.branch.referral

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.*

public class BillingGooglePlay {

    private lateinit var context_: Context
    var billingClient: BillingClient? = null

    public fun createBillingClient(context: Context) {
        context_ = context

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d("BranchSDK", "Branch billingClient setup finished.")
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BranchSDK", "Branch billingClient disconnected.")
            }
        })
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (PrefHelper.getInstance(context_).isAutoLogInAppPurchasesAsEventsEnabled()) {
//                        val event = BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
//                        event.logEventWithPurchase(
//                            Branch.getInstance().applicationContext,
//                            purchase!!
//                        )
                    }
                }
            }
        }

    /**
     * Logs a Branch Commerce Event based on an in-app purchase
     *
     * @param context  Current context
     * @param purchase Respective purchase
     */
    fun logEventWithPurchase(context: Context?, purchase: Purchase) {
        val productIds = purchase.products
        val productList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        val subsList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        Log.d("BranchSDK","Creating event for purchase with products: $productIds");

        for (productId: String? in productIds) {
            val inAppProduct = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId!!)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            productList.add(inAppProduct)

            val subsProduct = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            subsList.add(subsProduct)
        }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val querySubsProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subsList)
            .build()

        Branch.getInstance().billingClient.queryProductDetailsAsync(querySubsProductDetailsParams,
            ProductDetailsResponseListener { billingResult: BillingResult, subsProductDetailsList: List<ProductDetails?> ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val contentItemBUOs: MutableList<BranchUniversalObject> =
                        ArrayList()
                    val currency: CurrencyType = CurrencyType.USD
                    val revenue: Double = 0.00

                    for (product: ProductDetails? in subsProductDetailsList) {
                        val buo: BranchUniversalObject = createBUOWithSubsProductDetails(product)
                        contentItemBUOs.add(buo)
                    }
                    createAndLongEventForPurchase(
                        context_,
                        purchase,
                        contentItemBUOs,
                        currency,
                        revenue,
                        "Subscription"
                    )
                } else {
                    Log.e(
                        "BranchEvent",
                        "Failed to query subscriptions. Error code: " + billingResult.responseCode
                    )
                }
            })

        Branch.getInstance().billingClient.queryProductDetailsAsync(
            queryProductDetailsParams,
            (ProductDetailsResponseListener { billingResult: BillingResult, productDetailsList: List<ProductDetails?> ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val contentItemBUOs: MutableList<BranchUniversalObject> =
                        ArrayList()
                    val currency: CurrencyType = CurrencyType.USD
                    val revenue: Double = 0.00

                    for (product: ProductDetails? in productDetailsList) {
                        val buo: BranchUniversalObject = createBUOWithInAppProductDetails(product)
                        contentItemBUOs.add(buo)
                    }
                    createAndLongEventForPurchase(
                        context_,
                        purchase,
                        contentItemBUOs,
                        currency,
                        revenue,
                        "In-App Purchase"
                    )
                } else {
                    Log.e(
                        "BranchEvent",
                        "Failed to query products. Error code: " + billingResult.responseCode
                    )
                }
            })
        )
    }


    private fun createBUOWithInAppProductDetails(product: ProductDetails, quantity: Double): BranchUniversalObject {

        val currency = CurrencyType.valueOf(product.oneTimePurchaseOfferDetails!!.priceCurrencyCode)
        val price = (product.oneTimePurchaseOfferDetails!!.priceAmountMicros / 1000000).toDouble()
        val revenue = price * quantity

        return BranchUniversalObject()
            .setCanonicalIdentifier(product.productId)
            .setTitle(product.title)
            .setContentMetadata(
                ContentMetadata()
                    .addCustomMetadata("product_type", product.productType)
                    .setPrice(price, currency)
                    .setProductName(product.name)
                    .setQuantity(quantity)
                    .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
            )
    }

    private fun createBUOWithSubsProductDetails(product: ProductDetails): BranchUniversalObject {

        val purchasedProductType = "Subscription"
        val pricingPhaseList = product.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)

        val currency = pricingPhaseList?.let {
            CurrencyType.valueOf(
                it.priceCurrencyCode
            )
        }
        val price = ((pricingPhaseList?.priceAmountMicros ?: 0) / 1000000).toDouble()
        val quantity = 1
        val revenue = price * quantity

        return BranchUniversalObject()
            .setCanonicalIdentifier(product.productId)
            .setTitle(product.title)
            .setContentMetadata(
                ContentMetadata()
                    .addCustomMetadata("product_type", product.productType)
                    .setPrice(price, currency)
                    .setProductName(product.name)
                    .setQuantity(quantity.toDouble())
                    .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
            )
    }

    private fun createAndLongEventForPurchase(
        context: Context,
        purchase: Purchase,
        contentItems: List<BranchUniversalObject>,
        currency: CurrencyType,
        revenue: Double,
        productType: String
    ) {
        BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
            .setCurrency(currency)
            .setDescription(purchase.orderId)
            .setCustomerEventAlias(productType)
            .setRevenue(revenue)
            .addCustomDataProperty("package_name", purchase.packageName)
            .addCustomDataProperty("order_id", purchase.orderId)
            .addCustomDataProperty("loggedFromIAP", "true")
            .addCustomDataProperty("is_auto_renewing", purchase.isAutoRenewing.toString())
            .addCustomDataProperty("purchase_token", purchase.purchaseToken)
            .addContentItems(contentItems)
            .logEvent(context)
    }


}