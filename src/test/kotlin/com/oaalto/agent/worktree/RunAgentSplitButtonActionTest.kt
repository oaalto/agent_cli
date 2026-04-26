package com.oaalto.agent.worktree

import kotlin.test.Test
import kotlin.test.assertEquals

class RunAgentSplitButtonActionTest {
    @Test
    fun `default split button action runs in current project`() {
        val splitAction = RunAgentSplitButtonAction()
        val mainAction = splitAction.defaultMainActionForTests()

        assertEquals(RUN_IN_CURRENT_PROJECT_TEXT, mainAction.templatePresentation.text)
    }
}
