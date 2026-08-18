package com.prof18.feedflow.core.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpHostValidatorTest {

    @Test
    fun `malformed percent escapes are rejected`() {
        assertFalse(HttpHostValidator.isSafeForHttpClient("100%example.com"))
        assertFalse(HttpHostValidator.isSafeForHttpClient("example%.com"))
        assertFalse(HttpHostValidator.isSafeForHttpClient("example.com%"))
        assertFalse(HttpHostValidator.isSafeForHttpClient("example%zz.com"))
        assertFalse(HttpHostValidator.isSafeForHttpClient("example%2.com"))
    }

    @Test
    fun `well formed percent escapes are accepted`() {
        assertTrue(HttpHostValidator.isSafeForHttpClient("ex%C3%A4mple.com"))
        assertTrue(HttpHostValidator.isSafeForHttpClient("ex%c3%a4mple.com"))
    }

    @Test
    fun `colons and brackets are rejected outside an ipv6 literal`() {
        assertFalse(HttpHostValidator.isSafeForHttpClient("example.com:"))
        assertFalse(HttpHostValidator.isSafeForHttpClient("example.com:8080"))
        assertFalse(HttpHostValidator.isSafeForHttpClient("example].com"))
        assertFalse(HttpHostValidator.isSafeForHttpClient("[::1]extra["))
    }

    @Test
    fun `ipv6 literals are accepted`() {
        assertTrue(HttpHostValidator.isSafeForHttpClient("[::1]"))
        assertTrue(HttpHostValidator.isSafeForHttpClient("[2001:db8::1]"))
    }

    @Test
    fun `hosts ktor percent encodes on its own are accepted`() {
        assertTrue(HttpHostValidator.isSafeForHttpClient("münchen.example"))
        assertTrue(HttpHostValidator.isSafeForHttpClient("例え.jp"))
        assertTrue(HttpHostValidator.isSafeForHttpClient("ex_ample.com"))
        assertTrue(HttpHostValidator.isSafeForHttpClient("xn--bcher-kva.de"))
        assertTrue(HttpHostValidator.isSafeForHttpClient("example.com"))
    }

    @Test
    fun `blank hosts are rejected`() {
        assertFalse(HttpHostValidator.isSafeForHttpClient(""))
        assertFalse(HttpHostValidator.isSafeForHttpClient("   "))
    }

    @Test
    fun `url level check rejects unusable urls`() {
        assertFalse(HttpHostValidator.isUrlSafeForHttpClient(""))
        assertFalse(HttpHostValidator.isUrlSafeForHttpClient("   "))
        assertFalse(HttpHostValidator.isUrlSafeForHttpClient("https://100%example.com/feed"))
        assertTrue(HttpHostValidator.isUrlSafeForHttpClient("https://example.com/feed"))
        assertTrue(HttpHostValidator.isUrlSafeForHttpClient(" https://example.com/feed "))
    }
}
