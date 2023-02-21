package io.branch.referral

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.*

class BillingGooglePlay {

    private lateinit var context_: Context
    var billingClient: BillingClient? = null

    fun createBillingClient(context: Context, callback: (Boolean) -> Unit) {
        context_ = context

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BranchSDK", "Branch billingClient setup finished.")
                    callback(true)
                } else {
                    val errorMessage = "Billing client setup failed with error code: ${billingResult.responseCode}"
                    Log.w("BranchSDK", errorMessage)
                    callback(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BranchSDK", "Billing client disconnected")
                callback(false)
            }
        })
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (PrefHelper.getInstance(context_).isAutoLogInAppPurchasesAsEventsEnabled) {
                        logEventWithPurchase(context_, purchase)
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
    fun logEventWithPurchase(context: Context, purchase: Purchase) {
        val productIds = purchase.products
        val productList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        val subsList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        Log.d("BranchSDK", "Creating event for purchase with products: $productIds");

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

        billingClient?.queryProductDetailsAsync(
            querySubsProductDetailsParams
        ) { billingResult: BillingResult, subsProductDetailsList: List<ProductDetails?> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var contentItemBUOs: MutableList<BranchUniversalObject> =
                    ArrayList()
                var currency: CurrencyType = CurrencyType.USD
                var revenue = 0.00

                for (product: ProductDetails? in subsProductDetailsList) {
                    val buo: BranchUniversalObject = createBUOWithSubsProductDetails(product)

                    contentItemBUOs.add(buo)
                    revenue += buo.contentMetadata.price
                    currency = buo.contentMetadata.currencyType
                }

                if (contentItemBUOs.isNotEmpty()) {
                    createAndLongEventForPurchase(
                        context,
                        purchase,
                        contentItemBUOs,
                        currency,
                        revenue,
                        "Subscription"
                    )
                }
            } else {
                Log.e(
                    "BranchEvent",
                    "Failed to query subscriptions. Error code: " + billingResult.responseCode
                )
            }
        }

        billingClient?.queryProductDetailsAsync(
            queryProductDetailsParams
        ) { billingResult: BillingResult, productDetailsList: List<ProductDetails?> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                var contentItemBUOs: MutableList<BranchUniversalObject> =
                    ArrayList()
                var currency: CurrencyType = CurrencyType.USD
                var revenue = 0.00
                var quantity: Int = purchase.quantity

                for (product: ProductDetails? in productDetailsList) {
                    val buo: BranchUniversalObject =
                        createBUOWithInAppProductDetails(product, quantity)
                    contentItemBUOs.add(buo)
                    revenue += (buo.contentMetadata.price * quantity)
                    currency = buo.contentMetadata.currencyType
                }

                if (contentItemBUOs.isNotEmpty()) {
                    createAndLongEventForPurchase(
                        context,
                        purchase,
                        contentItemBUOs,
                        currency,
                        revenue,
                        "In-App Purchase"
                    )
                }
            } else {
                Log.e(
                    "BranchEvent",
                    "Failed to query products. Error code: " + billingResult.responseCode
                )
            }
        }
    }

    private fun createBUOWithSubsProductDetails(product: ProductDetails?): BranchUniversalObject {
        if (product != null) {

            val pricingPhaseList =
                product.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)

            val currency = pricingPhaseList?.let {
                CurrencyType.valueOf(
                    it.priceCurrencyCode
                )
            }

            val price = ((pricingPhaseList?.priceAmountMicros ?: 0) / 1000000).toDouble()

            return BranchUniversalObject()
                .setCanonicalIdentifier(product.productId)
                .setTitle(product.title)
                .setContentMetadata(
                    ContentMetadata()
                        .addCustomMetadata("product_type", product.productType)
                        .setPrice(price, currency)
                        .setProductName(product.name)
                        .setQuantity(1.0)
                        .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)

                )
        } else {
            return BranchUniversalObject()
        }
    }


    private fun createBUOWithInAppProductDetails(
        product: ProductDetails?,
        quantity: Int
    ): BranchUniversalObject {
        if (product != null) {

            val currency =
                CurrencyType.valueOf(product.oneTimePurchaseOfferDetails!!.priceCurrencyCode)
            val price =
                (product.oneTimePurchaseOfferDetails!!.priceAmountMicros / 1000000).toDouble()

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
        } else {
            return BranchUniversalObject()
        }
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
            .addCustomDataProperty("logged_from_IAP", "true")
            .addCustomDataProperty("is_auto_renewing", purchase.isAutoRenewing.toString())
            .addCustomDataProperty("purchase_token", purchase.purchaseToken)
            .addContentItems(contentItems)
            .logEvent(context)

        Log.d("BranchSDK", "Automatically logged IAP as Branch Event")
    }


}