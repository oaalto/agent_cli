package com.oaalto.agent.acp

import com.intellij.openapi.project.Project
import java.awt.Component
import java.awt.Container
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

internal fun fakeTranscriptProject(): Project =
    java.lang.reflect.Proxy.newProxyInstance(
        Project::class.java.classLoader,
        arrayOf(Project::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "isDisposed" -> false
            "getBasePath" -> null
            "toString" -> "FakeProject"
            else -> defaultTranscriptTestValue(method.returnType)
        }
    } as Project

private fun defaultTranscriptTestValue(returnType: Class<*>): Any? =
    when (returnType) {
        Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> false
        Int::class.javaPrimitiveType, Int::class.javaObjectType -> 0
        Long::class.javaPrimitiveType, Long::class.javaObjectType -> 0L
        else -> null
    }

internal fun runOnEdtSync(action: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
        action()
    } else {
        SwingUtilities.invokeAndWait(action)
    }
}

/**
 * Drains pending EDT work. Three iterations: [TranscriptViewController] schedules
 * apply and stick-to-bottom via `invokeLater`; one pass is often insufficient for
 * layout revalidation and scroll settle in headless runs.
 */
internal fun pumpTranscriptEdt() {
    repeat(3) {
        val latch = CountDownLatch(1)
        SwingUtilities.invokeLater { latch.countDown() }
        check(latch.await(5, TimeUnit.SECONDS)) { "timed out waiting for EDT" }
    }
}

internal fun <T : Component> collectTranscriptDescendants(
    root: Component,
    type: Class<T>,
): List<T> {
    val result = mutableListOf<T>()

    fun walk(component: Component) {
        if (type.isInstance(component)) {
            @Suppress("UNCHECKED_CAST")
            result += component as T
        }
        if (component is Container) {
            component.components.forEach(::walk)
        }
    }
    walk(root)
    return result
}
