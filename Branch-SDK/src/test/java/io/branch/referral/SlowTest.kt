package io.branch.referral

/**
 * JUnit category marker for tests that are slow by construction.
 *
 * These exercise a real wall-clock timeout rather than a mocked or advanced clock, so their
 * duration is inherent to what they prove and cannot be tuned down without weakening the test.
 * Mark them so a fast run can skip them:
 *
 * ```
 * ./gradlew :Branch-SDK:testDebugUnitTest -PexcludeSlowTests
 * ```
 *
 * Without the flag they run as usual, so the default build keeps full coverage.
 *
 * Usage:
 * ```
 * @Test
 * @Category(SlowTest::class)
 * fun someTestThatWaitsOutARealTimeout() { ... }
 * ```
 */
interface SlowTest
