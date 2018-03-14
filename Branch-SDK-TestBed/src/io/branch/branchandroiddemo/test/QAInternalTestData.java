package io.branch.branchandroiddemo.test;

import java.util.Date;
import java.util.Random;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Defines;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchContentSchema;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ProductCategory;

/**
 * Created by sojanpr on 3/11/18.
 * <p>
 *     Test data to be used by QA internal tests
 * </p>
 */

public class QAInternalTestData {
    public static BranchUniversalObject testBuo = new BranchUniversalObject()
            .setCanonicalIdentifier("myprod/1234")
            .setCanonicalUrl("https://test_canonical_url")
            .setContentDescription("est_content_description")
            .setContentExpiration(new Date(122323432444L))
            .setContentImageUrl("https://test_content_img_url")
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
            .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE)
            .setTitle("test_title")
            .setContentMetadata(
                    new ContentMetadata()
                            .addCustomMetadata("custom_metadata_key1", "custom_metadata_val1")
                            .addCustomMetadata("custom_metadata_key1", "custom_metadata_val1")
                            .addImageCaptions("image_caption_1", "image_caption2", "image_caption3")
                            .setAddress("Street_Name", "test city", "test_state", "test_country", "test_postal_code")
                            .setRating(5.2, 6.0, 5)
                            .setLocation(-151.67, -124.0)
                            .setPrice(10.0, CurrencyType.USD)
                            .setProductBrand("test_prod_brand")
                            .setProductCategory(ProductCategory.APPAREL_AND_ACCESSORIES)
                            .setProductName("test_prod_name")
                            .setProductCondition(ContentMetadata.CONDITION.EXCELLENT)
                            .setProductVariant("test_prod_variant")
                            .setQuantity(1.5)
                            .setSku("test_sku")
                            .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
            )
            .addKeyWord("keyword1")
            .addKeyWord("keyword2");
    
    public static LinkProperties linkProperties = new LinkProperties()
            .addControlParameter("test_cntrl_param_key", "test_cntrl_param_value")
            .addTag("test_tag")
            .setCampaign("test_campaign")
            .setChannel("test_channel")
            .setDuration(10000)
            .setStage("test_stage")
            .setFeature("test_feature");
    
    
    
    public static BranchEvent branchEvent = new BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
            .setAffiliation("test_affiliation")
            .setCoupon("test_coupon")
            .setCurrency(CurrencyType.USD)
            .setDescription("test_description")
            .setRevenue(1.2)
            .setSearchQuery("test_search_query")
            .setShipping(1.5)
            .setTax(1.8)
            .setTransactionID("test_transaction_id")
            .addCustomDataProperty("test_custom_data_key1", "test_custom_data_val1")
            .addCustomDataProperty("test_custom_data_key2", "test_custom_data_val2");
            
}
