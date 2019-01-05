package com.skide.utils

import org.junit.Test
import kotlin.test.assertEquals

class TestWebUtils {

    @Test
    fun testURLEncoder() {
        assertEquals("test=1&hello=world",
                encodeHTTPParams(mapOf("test" to "1", "hello" to "world")))
    }
}
