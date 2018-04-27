package io.branch.uitestbed.test.data;

import java.util.Date;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BranchContentSchema;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ProductCategory;

public class MockDataObjects {
    // Mock BUO
    public static BranchUniversalObject buo = new BranchUniversalObject()
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

    // Mock Link properties
    public static LinkProperties linkProperties = new LinkProperties()
            .addTag("myShareTag1")
            .addTag("myShareTag2")
            //.setAlias("mylinkName") // In case you need to white label your link
            .setChannel("myShareChannel2")
            .setFeature("mySharefeature2")
            .setStage("10")
            .setCampaign("Android campaign")
            .addControlParameter("$android_deeplink_path", "custom/path/*")
            .addControlParameter("$ios_url", "http://example.com/ios")
            .setDuration(100);
}
