package io.branch.referral

import io.branch.coroutines.readInstallReferrer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(JUnit4::class)
class InstallReferrerConnectionTests {
    private val ok = 0
    private val serviceUnavailable = 1
    private val callbackThreadName = "fake-main"

    // Stands in for the main thread.
    private val callbackThread: ExecutorService =
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, callbackThreadName) }

    private val readCount = AtomicInteger(0)
    private val endConnectionCount = AtomicInteger(0)
    private val connectionEnded = CountDownLatch(1)

    @After
    fun tearDown() {
        callbackThread.shutdownNow()
    }

    private fun endConnection() {
        endConnectionCount.incrementAndGet()
        connectionEnded.countDown()
    }

    private fun assertConnectionEnded() {
        assertTrue("endConnection was not called", connectionEnded.await(2, TimeUnit.SECONDS))
        assertEquals("endConnection call count", 1, endConnectionCount.get())
    }

    @Test
    fun readRunsOffTheCallbackThreadAndLeavesItFree() = runBlocking {
        val readThreadName = AtomicReference<String>()
        val callbackThreadWasFree = AtomicBoolean(false)

        val result = readInstallReferrer(
            startConnection = { onSetupFinished, _ -> callbackThread.execute { onSetupFinished(ok) } },
            isResponseOk = { it == ok },
            readReferrer = {
                readThreadName.set(Thread.currentThread().name)
                // Times out if the read runs on the callback thread.
                callbackThreadWasFree.set(runCatching { callbackThread.submit(Runnable {}).get(1, TimeUnit.SECONDS) }.isSuccess)
                "referrer"
            },
            endConnection = ::endConnection
        )

        assertEquals("referrer", result)
        assertNotEquals(callbackThreadName, readThreadName.get())
        assertTrue("callback thread was blocked during the read", callbackThreadWasFree.get())
        assertConnectionEnded()
    }

    @Test
    fun nonOkResponseReturnsNullWithoutReading() = runBlocking {
        val result = readInstallReferrer(
            startConnection = { onSetupFinished, _ -> callbackThread.execute { onSetupFinished(serviceUnavailable) } },
            isResponseOk = { it == ok },
            readReferrer = { readCount.incrementAndGet(); "referrer" },
            endConnection = ::endConnection
        )

        assertNull(result)
        assertEquals(0, readCount.get())
        assertConnectionEnded()
    }

    @Test
    fun setupCallbackOnTheCallingThreadIsHandled() = runBlocking {
        // Store clients call back inline when the service is unavailable.
        val result = readInstallReferrer(
            startConnection = { onSetupFinished, _ -> onSetupFinished(serviceUnavailable) },
            isResponseOk = { it == ok },
            readReferrer = { readCount.incrementAndGet(); "referrer" },
            endConnection = ::endConnection
        )

        assertNull(result)
        assertEquals(0, readCount.get())
        assertConnectionEnded()
    }

    @Test
    fun readExceptionReturnsNull() = runBlocking {
        val result = readInstallReferrer<String>(
            startConnection = { onSetupFinished, _ -> callbackThread.execute { onSetupFinished(ok) } },
            isResponseOk = { it == ok },
            readReferrer = { throw IllegalStateException("Service not connected. Please start a connection before using the service.") },
            endConnection = ::endConnection
        )

        assertNull(result)
        assertConnectionEnded()
    }

    @Test
    fun disconnectBeforeSetupReturnsNullAndEndsConnection() = runBlocking {
        val result = readInstallReferrer(
            startConnection = { _, onDisconnected -> callbackThread.execute { onDisconnected() } },
            isResponseOk = { it == ok },
            readReferrer = { readCount.incrementAndGet(); "referrer" },
            endConnection = ::endConnection
        )

        assertNull(result)
        assertEquals(0, readCount.get())
        assertConnectionEnded()
    }

    @Test
    fun setupAfterDisconnectIsIgnored() = runBlocking {
        val result = readInstallReferrer(
            startConnection = { onSetupFinished, onDisconnected ->
                callbackThread.execute {
                    onDisconnected()
                    onSetupFinished(ok)
                }
            },
            isResponseOk = { it == ok },
            readReferrer = { readCount.incrementAndGet(); "referrer" },
            endConnection = ::endConnection
        )

        assertNull(result)
        assertConnectionEnded()
        callbackThread.submit(Runnable {}).get(1, TimeUnit.SECONDS)
        assertEquals(0, readCount.get())
    }

    @Test
    fun startConnectionExceptionReturnsNullAndEndsConnection() = runBlocking {
        val result = readInstallReferrer(
            startConnection = { _, _ -> throw SecurityException("Not allowed to bind to service") },
            isResponseOk = { it == ok },
            readReferrer = { readCount.incrementAndGet(); "referrer" },
            endConnection = ::endConnection
        )

        assertNull(result)
        assertEquals(0, readCount.get())
        assertConnectionEnded()
    }

    @Test
    fun endConnectionExceptionDoesNotLoseTheReferrer() = runBlocking {
        val result = readInstallReferrer(
            startConnection = { onSetupFinished, _ -> callbackThread.execute { onSetupFinished(ok) } },
            isResponseOk = { it == ok },
            readReferrer = { "referrer" },
            endConnection = { throw IllegalArgumentException("Service not registered") }
        )

        assertEquals("referrer", result)
    }

    @Test
    fun rejectedHandOffReturnsNullWithoutThrowingOnTheCallbackThread() = runBlocking {
        val callbackError = AtomicReference<Throwable?>()

        val result = readInstallReferrer(
            startConnection = { onSetupFinished, _ ->
                callbackThread.execute {
                    try {
                        onSetupFinished(ok)
                    } catch (t: Throwable) {
                        callbackError.set(t)
                    }
                }
            },
            isResponseOk = { it == ok },
            readReferrer = { readCount.incrementAndGet(); "referrer" },
            endConnection = ::endConnection,
            readExecutor = Executor { throw RejectedExecutionException("rejected") }
        )

        assertNull(result)
        assertNull(callbackError.get())
        assertEquals(0, readCount.get())
        assertConnectionEnded()
    }

    @Test
    fun timeoutReturnsPromptlyWhileTheReadIsBlocked() = runBlocking {
        val releaseRead = CountDownLatch(1)
        try {
            val startedAt = System.nanoTime()

            val result = withTimeoutOrNull(200) {
                readInstallReferrer(
                    startConnection = { onSetupFinished, _ -> callbackThread.execute { onSetupFinished(ok) } },
                    isResponseOk = { it == ok },
                    // Bounded so a regression fails instead of hanging.
                    readReferrer = { releaseRead.await(5, TimeUnit.SECONDS); "late referrer" },
                    endConnection = ::endConnection
                )
            }
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

            assertNull(result)
            assertTrue("timeout took ${elapsedMs}ms", elapsedMs < 2_000)
            assertEquals("connection ended while the read was in flight", 1L, connectionEnded.count)
        } finally {
            releaseRead.countDown()
        }
        assertConnectionEnded()
    }

    @Test
    fun timeoutBeforeSetupEndsTheConnectionAndIgnoresALateSetup() = runBlocking {
        val lateSetup = AtomicReference<(Int) -> Unit>()

        val result = withTimeoutOrNull(200) {
            readInstallReferrer(
                startConnection = { onSetupFinished, _ -> lateSetup.set(onSetupFinished) },
                isResponseOk = { it == ok },
                readReferrer = { readCount.incrementAndGet(); "referrer" },
                endConnection = ::endConnection,
                // Inline, so a stray read would be counted.
                readExecutor = Executor { it.run() }
            )
        }

        assertNull(result)
        assertConnectionEnded()
        callbackThread.submit { lateSetup.get()(ok) }.get(1, TimeUnit.SECONDS)
        assertEquals(0, readCount.get())
        assertEquals("endConnection call count", 1, endConnectionCount.get())
    }
}
