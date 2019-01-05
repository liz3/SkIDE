package com.skide.utils

import org.junit.Test
import kotlin.test.assertEquals

class TestVersion {

    // This file is in fact testing java/com/skide/utils/Version and not kotlin!

    @Test
    fun testCompareTo() {
        val v1 = Version("1.0.0")
        val v2 = Version("1.0.0")
        val v3 = Version("1.0.1")
        val v4 = Version("1.1.1")
        val v5 = Version("0.1.1.1")

        assertEquals(v1, v2)

        assertEquals(0, v1.compareTo(v2)) // 1.0.0 = 1.0.0

        assertEquals(-1, v1.compareTo(v3)) // 1.0.0 < 1.0.1

        assertEquals(1, v3.compareTo(v1)) // 1.0.1 > 1.0.0

        assertEquals(1, v4.compareTo(v3)) // 1.1.1 < 1.0.1

        assertEquals(-1, v5.compareTo(v3)) // 0.1.1.1 < 1.0.1
    }
}
