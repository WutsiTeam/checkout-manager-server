package com.wutsi.checkout.manager.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NumberUtilTest {
    @Test
    fun toHumanReadableByteCountSI() {
        assertEquals("0 B", NumberUtil.toHumanReadableByteCountSI(0))
        assertEquals("27 B", NumberUtil.toHumanReadableByteCountSI(27))
        assertEquals("999 B", NumberUtil.toHumanReadableByteCountSI(999))
        assertEquals("1.0 KB", NumberUtil.toHumanReadableByteCountSI(1000))
        assertEquals("1.7 KB", NumberUtil.toHumanReadableByteCountSI(1728))
        assertEquals("20.0 MB", NumberUtil.toHumanReadableByteCountSI(20000100))
        assertEquals("1.9 TB", NumberUtil.toHumanReadableByteCountSI(1855425871872))
    }
}
