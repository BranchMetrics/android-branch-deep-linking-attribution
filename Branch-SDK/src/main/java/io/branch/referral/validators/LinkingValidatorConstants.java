package io.branch.referral.validators;

public class LinkingValidatorConstants {
        public static final String canonicalURLPromptText = "Please paste in a web link for the $canonical_url";
        public static final String deeplinkPathPromptText = "Please paste in a value for the $deeplink_path";
        public static final String customKeyPromptText = "Please enter your custom key and value for routing";
        public static final String step1ButtonText = "Next";
        public static final String step2ButtonText = "  Generate Links for Testing  ";
        public static final String step3ButtonText = "Done";
        public static final String canonicalUrlKey = "$canonical_url";
        public static final String deeplinkPathKey = "$deeplink_path";

        public static final String linkingValidatorRow1Title = "Link using App Link";
        public static final String linkingValidatorRow2Title = "Link using URI scheme";
        public static final String linkingValidatorRow3Title = "Web-only link";
        public static final String linkingValidatorRow4Title = "Link with missing data";

        public static final String infoButton1Copy = "Verifies that Universal Links / App Links are working correctly for your Branch domain";
        public static final String infoButton2Copy = "Verifies that URI schemes work correctly for your Branch domain";
        public static final String infoButton3Copy = "Verifies that web-only links are handled correctly to take you to the mobile web";
        public static final String infoButton4Copy = "Verifies that your app gracefully handles Branch links missing deep link data";
        public static final String infoButton5Copy = "Click the button to simulate a deep link click for the warm start use case";
        public static final String infoButton6Copy = "Click the button to simulate a deep link click for the foreground use case";

        public static final String debugButton1Copy = "Ensure you’ve entered the correct SHA 256s on the dashboard and added your Branch domains to the Android Manifest";
        public static final String debugButton2Copy = "Ensure that you’ve added a unique Branch URI scheme to the dashboard and Android Manifest";
        public static final String debugButton3Copy = "Ensure that your code checks for $web-only in the link data, and if it is true routes the user to the mobile web";
        public static final String debugButton4Copy = "Ensure that your code gracefully handles missing or invalid deep link data like taking them to the home screen";
        public static final String debugButton5Copy = "Ensure that you are initializing Branch inside of onStart() and that the code is called anytime the app enters the foreground";
        public static final String debugButton6Copy = "Ensure that you are calling reInit() inside of onNewIntent() after checking if branch_force_new_session is true";


}
