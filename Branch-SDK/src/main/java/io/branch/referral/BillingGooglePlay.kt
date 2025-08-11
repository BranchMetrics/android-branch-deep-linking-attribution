package io.branch.referral

import android.content.Context
import com.android.billingclient.api.*
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.*
import java.math.BigDecimal

class BillingGooglePlay private constructor() {

    lateinit var billingClient: BillingClient

    companion object {
        @Volatile
        private lateinit var instance: BillingGooglePlay

        fun getInstance(): BillingGooglePlay {
            synchronized(this) {
                if (!::instance.isInitialized) {
                    instance = BillingGooglePlay()

                    instance.billingClient =
                        BillingClient.newBuilder(Branch.getInstance().applicationContext)
                            .setListener(instance.purchasesUpdatedListener)
                            .enablePendingPurchases()
                            .build()
                }
                return instance
            }
        }
    }

    fun startBillingClient(callback: (Boolean) -> Unit) {
        if (billingClient.isReady) {
            BranchLogger.v("Billing Client has already been started..")
            callback(true)
        } else {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        BranchLogger.v("Billing Client setup finished.")
                        callback(true)
                    } else {
                        val errorMessage =
                            "Billing Client setup failed with error: ${billingResult.debugMessage}"
                        BranchLogger.e(errorMessage)
                        callback(false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    BranchLogger.w("Billing Client disconnected")
                    callback(false)
                }
            })
        }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { _, _ -> }

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

        billingClient.queryProductDetailsAsync(
            querySubsProductDetailsParams
        ) { billingResult: BillingResult, subsProductDetailsList: List<ProductDetails?> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val contentItemBUOs: MutableList<BranchUniversalObject> =
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
                    createAndLogEventForPurchase(
                        context,
                        purchase,
                        contentItemBUOs,
                        currency,
                        revenue,
                        BillingClient.ProductType.SUBS
                    )
                }
            }
            else {
                BranchLogger.e("Failed to query subscriptions. Error: " + billingResult.debugMessage)
            }
        }

        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams
        ) { billingResult: BillingResult, productDetailsList: List<ProductDetails?> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                val contentItemBUOs: MutableList<BranchUniversalObject> =
                    ArrayList()
                var currency: CurrencyType = CurrencyType.USD
                var revenue = 0.00
                val quantity: Int = purchase.quantity

                for (product: ProductDetails? in productDetailsList) {
                    val buo: BranchUniversalObject =
                        createBUOWithInAppProductDetails(product, quantity)
                    contentItemBUOs.add(buo)

                    revenue += (BigDecimal(buo.contentMetadata.price.toString()) * BigDecimal(
                        quantity.toString()
                    )).toDouble()
                    currency = buo.contentMetadata.currencyType
                }

                if (contentItemBUOs.isNotEmpty()) {
                    createAndLogEventForPurchase(
                        context,
                        purchase,
                        contentItemBUOs,
                        currency,
                        revenue,
                        BillingClient.ProductType.INAPP
                    )
                }
            }
            else {
                BranchLogger.e("Failed to query subscriptions. Error: " + billingResult.debugMessage)
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

            val price = pricingPhaseList?.priceAmountMicros?.div(1000000.0)

            val buo = BranchUniversalObject()
                .setCanonicalIdentifier(product.productId)
                .setTitle(product.title)

            val contentMetadata = ContentMetadata()
                .addCustomMetadata("product_type", product.productType)
                .setProductName(product.name)
                .setQuantity(1.0)
                .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)

            if (price != null && currency != null) {
                contentMetadata.setPrice(price, currency);
            }

            buo.contentMetadata = contentMetadata;

            return buo;
        } else {
            return BranchUniversalObject()
        }
    }

    private fun createBUOWithInAppProductDetails(
        product: ProductDetails?,
        quantity: Int
    ): BranchUniversalObject {
        if (product != null) {

            val currency = product.oneTimePurchaseOfferDetails?.priceCurrencyCode?.let {
                CurrencyType.valueOf(it)
            }
            val price = product.oneTimePurchaseOfferDetails?.priceAmountMicros?.div(1000000.0)

            val buo = BranchUniversalObject()
                .setCanonicalIdentifier(product.productId)
                .setTitle(product.title)

            val contentMetadata = ContentMetadata()
                .addCustomMetadata("product_type", product.productType)
                .setProductName(product.name)
                .setQuantity(quantity.toDouble())
                .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)

            if (price != null && currency != null) {
                contentMetadata.setPrice(price, currency);
            }

            buo.contentMetadata = contentMetadata;

            return buo;
        } else {
            return BranchUniversalObject()
        }
    }

    fun createAndLogEventForPurchase(
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

        BranchLogger.i("Successfully logged in-app purchase as Branch Event")
    }
}