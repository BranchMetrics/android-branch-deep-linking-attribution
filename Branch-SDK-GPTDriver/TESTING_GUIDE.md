# GPTDriver Test Writing Guide — Hybrid Approach

> **Rule: Deterministic first, AI only when necessary.**

## Principle

Every test step should use Espresso native actions by default. GPTDriver AI (`driver.execute()`, `driver.assertCondition()`) is reserved for scenarios where Espresso alone cannot handle the interaction — dynamic content, visual validation, or complex navigation that depends on screen state.

## Decision Matrix

| Scenario | Use | Example |
|----------|-----|---------|
| Tap a button with known text | Espresso | `onView(withText("Create Branch Link")).perform(click())` |
| Tap a button with known ID | Espresso | `onView(withId(R.id.cmdRefreshShortURL)).perform(click())` |
| Type text into a field | Espresso | `onView(withId(R.id.editText)).perform(typeText("test_user"))` |
| Scroll to a view | Espresso | `onView(withId(R.id.cmdCommerceEvent)).perform(scrollTo(), click())` |
| Assert exact text match | Espresso | `onView(withId(R.id.editReferralShortUrl)).check(matches(withSubstring("bnctestbed")))` |
| Assert view is displayed | Espresso | `onView(withText("Your QR Code")).check(matches(isDisplayed()))` |
| Wait for async result | Espresso | `IdlingResource` or `Thread.sleep()` as last resort |
| Validate dynamic/visual content | **AI** | `driver.assertCondition("The QR code image is visible in the dialog")` |
| Navigate through unknown state | **AI** | `driver.execute("If a dialog is open, dismiss it and return to main screen")` |
| Extract data from screen | **AI** | `driver.extract(listOf("url", "status_code"))` |
| Validate multiple conditions at once | **AI** | `driver.assertBulk(listOf("URL is visible", "URL starts with https"))` |

## TestBed Resource IDs

### Linking
| Button Text | Resource ID |
|-------------|-------------|
| Create Branch Link | `R.id.cmdRefreshShortURL` |
| Share Branch Link | `R.id.share_btn` |
| Native Share Branch Link | `R.id.native_share_btn` |
| Create QR Code | `R.id.qrCode_btn` |

### Data
| Button Text | Resource ID |
|-------------|-------------|
| View First Referring Params | `R.id.cmdPrintInstallParam` |
| View Latest Referring Params | `R.id.cmdPrintLatestParam` |
| Set User ID | `R.id.cmdIdentifyUser` |
| Clear User ID | `R.id.cmdClearUser` |
| Disable/Enable Tracking | `R.id.tracking_cntrl_btn` |
| Consumer Protection Preference | `R.id.cmdConsumerProtectionPreference` |

### Events
| Button Text | Resource ID |
|-------------|-------------|
| Send Commerce Event | `R.id.cmdCommerceEvent` |
| Send Content Event | `R.id.cmdContentEvent` |
| Send Lifecycle Event | `R.id.cmdLifecycleEvent` |
| Register View | `R.id.report_view_btn` |
| Test In-App Purchase | `R.id.cmdInAppPurchase` |

### Misc
| Button Text | Resource ID |
|-------------|-------------|
| Init Session | `R.id.initSessionButton` |
| Browser Test | `R.id.openInAppBrowser` |
| Load Branch Logs | `R.id.viewLogsButton` |
| Send Notification | `R.id.notif_btn` |
| Simulate Logout | `R.id.logout_btn` |
| Simulate Plugin Notify Init | `R.id.notifyInit_btn` |
| Settings | `R.id.settings_btn` |

### Other Views
| View | Resource ID |
|------|-------------|
| URL text field | `R.id.editReferralShortUrl` |
| Log output text | `R.id.logOutputTextView` |

## Imports Template

```kotlin
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
```

## Example: Hybrid Test

```kotlin
class LinkCreationTest : BaseGptDriverTest() {

    @Test
    fun createBranchLink_generatesValidUrl() {
        // DETERMINISTIC: tap button by resource ID
        onView(withId(R.id.cmdRefreshShortURL)).perform(click())

        // DETERMINISTIC: wait for async link generation
        Thread.sleep(3000)

        // DETERMINISTIC: assert URL contains expected domain
        onView(withId(R.id.editReferralShortUrl))
            .check(matches(withSubstring("bnctestbed.app.link")))

        // AI: only if we need to validate something visual/contextual
        // driver.assertCondition("The generated URL is fully visible and not truncated")

        driver.setSessionStatus("success")
    }
}
```

## GPTDriver API Reference

| Method | Type | When to Use |
|--------|------|-------------|
| `driver.execute(instruction)` | AI | Complex/dynamic navigation Espresso can't express |
| `driver.assertCondition(condition)` | AI | Visual/contextual validation beyond text matching |
| `driver.assertBulk(conditions)` | AI | Multiple visual assertions in one call |
| `driver.checkBulk(conditions)` | AI | Non-throwing multi-condition check (returns Map) |
| `driver.extract(fields)` | AI | Extract structured data from screen |
| `driver.setSessionStatus(status)` | Control | Mark test result for MobileBoost dashboard |
