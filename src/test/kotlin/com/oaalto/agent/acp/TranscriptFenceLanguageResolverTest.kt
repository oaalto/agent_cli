package com.oaalto.agent.acp

import com.intellij.openapi.fileTypes.PlainTextFileType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TranscriptFenceLanguageResolverTest {
    @Test
    fun `kotlin alias normalizes to kt extension`() {
        assertEquals("kt", TranscriptFenceLanguageResolver.normalizeExtension("kotlin"))
    }

    @Test
    fun `kt tag keeps kt extension`() {
        assertEquals("kt", TranscriptFenceLanguageResolver.normalizeExtension("kt"))
    }

    @Test
    fun `json tag keeps json extension`() {
        assertEquals("json", TranscriptFenceLanguageResolver.normalizeExtension("json"))
    }

    @Test
    fun `unknown tag keeps normalized value`() {
        assertEquals(
            "not-a-real-language-tag",
            TranscriptFenceLanguageResolver.normalizeExtension("not-a-real-language-tag"),
        )
    }

    @Test
    fun `empty tag normalizes to null`() {
        assertEquals(null, TranscriptFenceLanguageResolver.normalizeExtension(""))
    }

    @Test
    fun `bash alias normalizes to sh extension`() {
        assertEquals("sh", TranscriptFenceLanguageResolver.normalizeExtension("bash"))
    }

    @Test
    fun `unknown language resolves without throwing`() {
        val fileType = TranscriptFenceLanguageResolver.resolveFileType("not-a-real-language-tag")
        assertNotEquals("", fileType.name)
    }

    @Test
    fun `kotlin resolves to non plain text when platform registers kotlin`() {
        val fileType = TranscriptFenceLanguageResolver.resolveFileType("kotlin")
        assertNotEquals(PlainTextFileType.INSTANCE, fileType)
    }
}
