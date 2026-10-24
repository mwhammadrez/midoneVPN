package dev.tunnelkit.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class SubscriptionFetcherTest {

    private val publicResolver: (String) -> List<InetAddress> =
        { listOf(InetAddress.getByName("93.184.216.34")) }
    private val privateResolver: (String) -> List<InetAddress> =
        { listOf(InetAddress.getByName("192.168.1.10")) }
    private val loopbackResolver: (String) -> List<InetAddress> =
        { listOf(InetAddress.getByName("127.0.0.1")) }

    private val link = "vless://5783a3e7-e373-51cd-8642-c83782b807c5@1.2.3.4:443?security=tls&sni=a.com#A"

    private class FakeHttp(
        private val body: String,
        private val status: Int = 200,
        private val finalUrl: String? = null,
    ) : HttpFetcher {
        var calls = 0
        override fun get(url: String, maxBytes: Int): HttpFetcher.Response {
            calls++
            return HttpFetcher.Response(status, body, finalUrl ?: url)
        }
    }

    @Test
    fun happyPathParsesAndCaches() {
        var cached: String? = null
        val f = SubscriptionFetcher(
            http = FakeHttp(link),
            resolver = publicResolver,
            onCacheWrite = { _, body -> cached = body },
        )
        val outcome = f.refresh("https://example.com/sub.txt")
        assertTrue(outcome is SubscriptionFetcher.Outcome.Fresh)
        assertEquals(1, (outcome as SubscriptionFetcher.Outcome.Fresh).parsed.links.size)
        assertEquals(link, cached)
    }

    @Test
    fun rejectsPlainHttp() {
        val f = SubscriptionFetcher(FakeHttp(link), publicResolver)
        val o = f.refresh("http://example.com/sub.txt")
        assertTrue(o is SubscriptionFetcher.Outcome.Failed)
        assertTrue((o as SubscriptionFetcher.Outcome.Failed).reason.contains("scheme"))
    }

    @Test
    fun rejectsPrivateTarget() {
        val f = SubscriptionFetcher(FakeHttp(link), privateResolver)
        val o = f.refresh("https://internal.example/sub.txt")
        assertTrue(o is SubscriptionFetcher.Outcome.Failed)
        assertTrue((o as SubscriptionFetcher.Outcome.Failed).reason.contains("non-public"))
    }

    @Test
    fun rejectsLoopbackHost() {
        val f = SubscriptionFetcher(FakeHttp(link), loopbackResolver)
        val o = f.refresh("https://localhost/sub.txt")
        assertTrue(o is SubscriptionFetcher.Outcome.Failed)
    }

    @Test
    fun rejectsRedirectToPrivate() {
        val f = SubscriptionFetcher(
            http = FakeHttp(link, finalUrl = "https://192.168.0.5/sub.txt"),
            resolver = { host ->
                if (host == "192.168.0.5") listOf(InetAddress.getByName("192.168.0.5"))
                else listOf(InetAddress.getByName("93.184.216.34"))
            },
        )
        val o = f.refresh("https://example.com/sub.txt")
        assertTrue(o is SubscriptionFetcher.Outcome.Failed)
        assertTrue((o as SubscriptionFetcher.Outcome.Failed).reason.contains("redirect"))
    }

    @Test
    fun fallsBackToLastGoodOnHttpError() {
        val f = SubscriptionFetcher(
            http = FakeHttp("<html>blocked</html>", status = 403),
            resolver = publicResolver,
            onCacheRead = { link },
        )
        val o = f.refresh("https://example.com/sub.txt")
        assertTrue(o is SubscriptionFetcher.Outcome.FromCache)
        assertEquals(1, (o as SubscriptionFetcher.Outcome.FromCache).parsed.links.size)
    }

    @Test
    fun failsWhenNothingCached() {
        val f = SubscriptionFetcher(FakeHttp("nope"), publicResolver)
        assertTrue(f.refresh("https://example.com/sub.txt") is SubscriptionFetcher.Outcome.Failed)
    }
}
