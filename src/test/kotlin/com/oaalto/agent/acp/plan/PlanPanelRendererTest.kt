package com.oaalto.agent.acp.plan

import com.oaalto.agent.acp.PlanEntry
import com.oaalto.agent.acp.PlanEntryPriority
import com.oaalto.agent.acp.PlanEntryStatus
import com.oaalto.agent.acp.PlanVariant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class PlanPanelRendererTest {
    @Test
    fun `renders pending status with empty checkbox icon`() {
        val entries =
            listOf(
                PlanEntry(
                    content = "Test task",
                    status = PlanEntryStatus.PENDING,
                    priority = PlanEntryPriority.MEDIUM,
                ),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertContains(html, "[ ]")
        assertContains(html, "Test task")
    }

    @Test
    fun `renders in-progress status with arrow icon`() {
        val entries =
            listOf(
                PlanEntry(
                    content = "Active task",
                    status = PlanEntryStatus.IN_PROGRESS,
                    priority = PlanEntryPriority.MEDIUM,
                ),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertContains(html, "[→]")
        assertContains(html, "Active task")
    }

    @Test
    fun `renders completed status with checkmark icon`() {
        val entries =
            listOf(
                PlanEntry(
                    content = "Done task",
                    status = PlanEntryStatus.COMPLETED,
                    priority = PlanEntryPriority.MEDIUM,
                ),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertContains(html, "[✓]")
        assertContains(html, "Done task")
    }

    @Test
    fun `renders high priority with bold styling`() {
        val entries =
            listOf(
                PlanEntry(
                    content = "Important task",
                    status = PlanEntryStatus.PENDING,
                    priority = PlanEntryPriority.HIGH,
                ),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertContains(html, "Important task")
        assertContains(html, "font-weight:bold")
    }

    @Test
    fun `renders low priority with muted color`() {
        val entries =
            listOf(
                PlanEntry(
                    content = "Optional task",
                    status = PlanEntryStatus.PENDING,
                    priority = PlanEntryPriority.LOW,
                ),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertContains(html, "Optional task")
        assertContains(html, "#999999")
    }

    @Test
    fun `renders progress summary with completed count`() {
        val entries =
            listOf(
                PlanEntry("Task 1", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
                PlanEntry("Task 2", PlanEntryStatus.IN_PROGRESS, PlanEntryPriority.MEDIUM),
                PlanEntry("Task 3", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertContains(html, "1 of 3 completed")
    }

    @Test
    fun `renders all completed summary with success color`() {
        val entries =
            listOf(
                PlanEntry("Task 1", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
                PlanEntry("Task 2", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertContains(html, "2 of 2 completed")
        // Check for hex color pattern (dynamic color from provider)
        assertTrue(html.contains(Regex("color:#[0-9a-f]{6}")), "Expected hex color in style attribute")
    }

    @Test
    fun `escapes HTML special characters in content`() {
        val entries =
            listOf(
                PlanEntry(
                    content = "Task with <script>alert('xss')</script> & more",
                    status = PlanEntryStatus.PENDING,
                    priority = PlanEntryPriority.MEDIUM,
                ),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertTrue(!html.contains("<script>"), "Should not contain unescaped script tag")
        assertContains(html, "&lt;script&gt;")
        assertContains(html, "&amp;")
    }

    @Test
    fun `renders numbered entries in order`() {
        val entries =
            listOf(
                PlanEntry("First task", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                PlanEntry("Second task", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                PlanEntry("Third task", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
            )

        val html = PlanPanelRenderer.render("plan-1", entries)

        assertTrue(html.indexOf("1. First task") < html.indexOf("2. Second task"))
        assertTrue(html.indexOf("2. Second task") < html.indexOf("3. Third task"))
    }

    @Test
    fun `renders empty entries list with placeholder`() {
        val html = PlanPanelRenderer.render("plan-1", emptyList())

        assertContains(html, "[empty plan]")
    }

    @Test
    fun `renders file variant as reference line`() {
        val html =
            PlanPanelRenderer.render(
                "plan-1",
                emptyList(),
                PlanVariant.File("file:///path/to/plan.md"),
            )

        assertContains(html, "[plan file]")
        assertContains(html, "file:///path/to/plan.md")
    }

    @Test
    fun `renders markdown variant with truncated content`() {
        val markdown = "# Plan\nStep 1: Do something\nStep 2: Do more\nStep 3: Finish\nStep 4: Extra"

        val html =
            PlanPanelRenderer.render(
                "plan-1",
                emptyList(),
                PlanVariant.Markdown(markdown),
            )

        // First 3 lines should be: # Plan, Step 1: Do something, Step 2: Do more
        assertContains(html, "Plan (markdown)")
        assertContains(html, "Step 1")
        assertTrue(html.contains("Step 2"), "Expected 'Step 2' in HTML but was: $html")
        assertTrue(!html.contains("Step 3"), "Step 3 should not be in first 3 lines")
        assertTrue(!html.contains("Step 4"), "Should only show first 3 lines")
        assertContains(html, "…")
    }

    @Test
    fun `includes plan ID in element ID attribute`() {
        val entries =
            listOf(
                PlanEntry("Task", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
            )

        val html = PlanPanelRenderer.render("my-plan-id", entries)

        assertContains(html, "id=\"plan-my-plan-id\"")
    }

    @Test
    fun `escapes plan ID in element ID attribute`() {
        val entries =
            listOf(
                PlanEntry("Task", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
            )

        val html = PlanPanelRenderer.render("plan<id>&\"", entries)

        assertTrue(!html.contains("id=\"plan-plan<id>&\"\"\""))
        assertContains(html, "plan-plan&lt;id&gt;&amp;&quot;")
    }
}
