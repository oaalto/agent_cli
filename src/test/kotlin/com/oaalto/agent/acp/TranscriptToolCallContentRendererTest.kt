package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.EmbeddedResourceResource
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranscriptToolCallContentRendererTest {
    @Test
    fun `completed text content renders escaped pre block`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = listOf(ToolCallContent.Content(ContentBlock.Text("line one\nline two"))),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("<pre"))
        assertTrue(fragments[0].contains("border-left:2px solid #444444"))
        assertTrue(fragments[0].contains("color:#999999"))
        assertTrue(fragments[0].contains("line one"))
        assertTrue(fragments[0].contains("line two"))
    }

    @Test
    fun `failed text content renders body`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = listOf(ToolCallContent.Content(ContentBlock.Text("error output"))),
                status = ToolCallStatus.FAILED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("error output"))
    }

    @Test
    fun `in progress status emits no content body`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = listOf(ToolCallContent.Content(ContentBlock.Text("partial"))),
                status = ToolCallStatus.IN_PROGRESS,
            )

        assertTrue(fragments.isEmpty())
    }

    @Test
    fun `pending status emits no content body`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = listOf(ToolCallContent.Content(ContentBlock.Text("waiting"))),
                status = ToolCallStatus.PENDING,
            )

        assertTrue(fragments.isEmpty())
    }

    @Test
    fun `null status emits no content body`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = listOf(ToolCallContent.Content(ContentBlock.Text("ignored"))),
                status = null,
            )

        assertTrue(fragments.isEmpty())
    }

    @Test
    fun `empty text content produces no fragment`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = listOf(ToolCallContent.Content(ContentBlock.Text(""))),
                status = ToolCallStatus.COMPLETED,
            )

        assertTrue(fragments.isEmpty())
    }

    @Test
    fun `null content list produces no fragments`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = null,
                status = ToolCallStatus.COMPLETED,
            )

        assertTrue(fragments.isEmpty())
    }

    @Test
    fun `diff renders escaped path and colored lines`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Diff(
                            path = "src/Foo.kt",
                            newText = "line b\nline c",
                            oldText = "line a\nline b",
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        val html = fragments[0]
        assertTrue(html.contains("src/Foo.kt"))
        assertTrue(html.contains("color:#c43c3c"))
        assertTrue(html.contains("- line a"))
        assertTrue(html.contains("color:#2d8a4e"))
        assertTrue(html.contains("+ line c"))
    }

    @Test
    fun `diff with only additions shows green lines`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Diff(
                            path = "new.txt",
                            newText = "hello",
                            oldText = null,
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("+ hello"))
        assertTrue(!fragments[0].contains("color:#c43c3c"))
    }

    @Test
    fun `diff with empty oldText string shows additions only`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Diff(
                            path = "new.txt",
                            newText = "hello",
                            oldText = "",
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("+ hello"))
        assertTrue(!fragments[0].contains("color:#c43c3c"))
        assertTrue(!fragments[0].contains("- "))
    }

    @Test
    fun `diff middle insertion does not mis-report unchanged trailing lines`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Diff(
                            path = "file.txt",
                            newText = "a\nx\nb\nc",
                            oldText = "a\nb\nc",
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        val html = fragments[0]
        assertTrue(html.contains("+ x"))
        assertTrue(!html.contains("- b"))
        assertTrue(!html.contains("- c"))
        assertTrue(!html.contains("+ b"))
        assertTrue(!html.contains("+ c"))
    }

    @Test
    fun `diff replacement shows remove and add without re-adding unchanged tail`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Diff(
                            path = "file.txt",
                            newText = "x\nb\nc",
                            oldText = "a\nb\nc",
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        val html = fragments[0]
        assertTrue(html.contains("- a"))
        assertTrue(html.contains("+ x"))
        assertTrue(!html.contains("- b"))
        assertTrue(!html.contains("- c"))
        assertTrue(!html.contains("+ b"))
        assertTrue(!html.contains("+ c"))
    }

    @Test
    fun `diff with only removals shows red lines`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Diff(
                            path = "gone.txt",
                            newText = "",
                            oldText = "removed",
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("- removed"))
        assertTrue(!fragments[0].contains("color:#2d8a4e"))
    }

    @Test
    fun `terminal reference renders muted id line`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = listOf(ToolCallContent.Terminal(terminalId = "term-7")),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("[terminal output] id=term-7"))
        assertTrue(fragments[0].contains("color:#999999"))
    }

    @Test
    fun `image content renders placeholder without base64`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Content(
                            ContentBlock.Image(
                                data = "aGVsbG8=",
                                mimeType = "image/png",
                                uri = null,
                            ),
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("[image: image/png]"))
        assertTrue(!fragments[0].contains("aGVsbG8="))
    }

    @Test
    fun `audio content renders placeholder`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Content(
                            ContentBlock.Audio(
                                data = "audio-bytes",
                                mimeType = "audio/wav",
                            ),
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("[audio: audio/wav]"))
        assertTrue(!fragments[0].contains("audio-bytes"))
    }

    @Test
    fun `resource link renders name and uri`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Content(
                            ContentBlock.ResourceLink(
                                name = "README",
                                uri = "file:///readme.md",
                            ),
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("README (file:///readme.md)"))
    }

    @Test
    fun `text embedded resource renders like text body`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Content(
                            ContentBlock.Resource(
                                EmbeddedResourceResource.TextResourceContents(
                                    text = "embedded text",
                                    uri = "file:///x.txt",
                                ),
                            ),
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("<pre"))
        assertTrue(fragments[0].contains("embedded text"))
    }

    @Test
    fun `binary embedded resource renders compact placeholder`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Content(
                            ContentBlock.Resource(
                                EmbeddedResourceResource.BlobResourceContents(
                                    blob = "aGVsbG8=",
                                    uri = "file:///bin.dat",
                                ),
                            ),
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("[binary resource: file:///bin.dat]"))
        assertTrue(!fragments[0].contains("aGVsbG8="))
    }

    @Test
    fun `multiple content entries render in order`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Content(ContentBlock.Text("first")),
                        ToolCallContent.Terminal(terminalId = "t1"),
                        ToolCallContent.Content(ContentBlock.Text("second")),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(3, fragments.size)
        assertTrue(fragments[0].contains("first"))
        assertTrue(fragments[1].contains("[terminal output] id=t1"))
        assertTrue(fragments[2].contains("second"))
    }

    @Test
    fun `text content escapes html special characters`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Content(
                            ContentBlock.Text("<script>alert(1)</script> & \"quotes\""),
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        val html = fragments[0]
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&amp;"))
        assertTrue(html.contains("&quot;quotes&quot;"))
        assertTrue(!html.contains("<script>"))
    }

    @Test
    fun `diff lines escape html special characters`() {
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content =
                    listOf(
                        ToolCallContent.Diff(
                            path = "x<&>.txt",
                            newText = "<added>",
                            oldText = "<removed>",
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        val html = fragments[0]
        assertTrue(html.contains("x&lt;&amp;&gt;.txt"))
        assertTrue(html.contains("- &lt;removed&gt;"))
        assertTrue(html.contains("+ &lt;added&gt;"))
    }

    @Test
    fun `very long text truncates with character total suffix`() {
        val longText = "x".repeat(TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS + 100)
        val fragments =
            TranscriptToolCallContentRenderer.renderContentFragments(
                content = listOf(ToolCallContent.Content(ContentBlock.Text(longText))),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("… (truncated, ${longText.length} characters total)"))
        assertTrue(fragments[0].length < longText.length)
    }

    @Test
    fun `fenced kotlin tool text emits highlighted code body part`() {
        val parts =
            TranscriptToolCallContentRenderer.renderBodyParts(
                content =
                    listOf(
                        ToolCallContent.Content(
                            ContentBlock.Text("```kotlin\nfun main()\n```"),
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(1, parts.size)
        val code = assertIs<TranscriptBodyPart.Code>(parts.single())
        assertEquals("kotlin", code.languageId)
        assertEquals("fun main()", code.code)
    }

    @Test
    fun `plain log text still emits html pre body part`() {
        val parts =
            TranscriptToolCallContentRenderer.renderBodyParts(
                content = listOf(ToolCallContent.Content(ContentBlock.Text("error output"))),
                status = ToolCallStatus.FAILED,
            )

        assertEquals(1, parts.size)
        val html = assertIs<TranscriptBodyPart.Html>(parts.single())
        assertTrue(html.fragment.contains("<pre"))
        assertTrue(html.fragment.contains("error output"))
    }

    @Test
    fun `mixed diff and fenced text preserves both renderers`() {
        val parts =
            TranscriptToolCallContentRenderer.renderBodyParts(
                content =
                    listOf(
                        ToolCallContent.Diff(
                            path = "Foo.kt",
                            newText = "new",
                            oldText = "old",
                        ),
                        ToolCallContent.Content(
                            ContentBlock.Text("```json\n{\"ok\":true}\n```"),
                        ),
                    ),
                status = ToolCallStatus.COMPLETED,
            )

        assertEquals(2, parts.size)
        val diff = assertIs<TranscriptBodyPart.Html>(parts[0])
        assertTrue(diff.fragment.contains("color:#c43c3c"))
        val code = assertIs<TranscriptBodyPart.Code>(parts[1])
        assertEquals("json", code.languageId)
    }
}
