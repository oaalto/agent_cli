package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptAgentFenceNormalizerTest {
    @Test
    fun `leaves well formed fences unchanged`() {
        val input =
            """intro

```python
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n - 1) + fibonacci(n - 2)
```

Example: `fibonacci(10)` → `55`."""
        assertEquals(input, normalizeAgentFences(input))
    }

    @Test
    fun `splits inline closing fence from trailing prose`() {
        val input =
            """```python
return fibonacci(n-1) + fibonacci(n-2)```Example: `fibonacci(10)` → `55`."""
        val expected =
            """```python
return fibonacci(n-1) + fibonacci(n-2)
```
Example: `fibonacci(10)` → `55`."""
        assertEquals(expected, normalizeAgentFences(input))
    }

    @Test
    fun `splits closing fence line with trailing prose`() {
        val input =
            """```python
return fibonacci(n-1) + fibonacci(n-2)
```Example: `fibonacci(10)` → `55`."""
        val expected =
            """```python
return fibonacci(n-1) + fibonacci(n-2)
```
Example: `fibonacci(10)` → `55`."""
        assertEquals(expected, normalizeAgentFences(input))
    }

    @Test
    fun `splits opening fence when language tag merges with first code line`() {
        val input =
            """```kotlinfun summarize(numbers: List<Number>) {
 val total = numbers.sumOf { it.toDouble() }
 println(total)
}
```Example:"""
        val expected =
            """```kotlin
fun summarize(numbers: List<Number>) {
 val total = numbers.sumOf { it.toDouble() }
 println(total)
}
```
Example:"""
        assertEquals(expected, normalizeAgentFences(input))
    }

    @Test
    fun `splits single line opening fence when language tag merges with code`() {
        val input = "```kotlinsummarize(listOf(1,2,3,4.5)) // prints10.5```"
        val expected =
            """```kotlin
summarize(listOf(1,2,3,4.5)) // prints10.5
```"""
        assertEquals(expected, normalizeAgentFences(input))
    }

    @Test
    fun `auto closes unclosed fence at end of input`() {
        val dollar = "$"
        val input =
            """```kotlin
fun concat(a: String, b: String): String = ${dollar}a${dollar}b
"""
        val expected =
            """```kotlin
fun concat(a: String, b: String): String = ${dollar}a${dollar}b

```"""
        assertEquals(expected, normalizeAgentFences(input))
    }

    @Test
    fun `splits inline open and close fence with trailing prose on same line`() {
        val input = "```kotlinfun concat(a: String, b: String): String = a + b```Or with string templates:"
        val expected =
            """```kotlin
fun concat(a: String, b: String): String = a + b
```
Or with string templates:"""
        assertEquals(expected, normalizeAgentFences(input))
    }

    @Test
    fun `splits opening fence when prose precedes fence on same line`() {
        val input =
            """Here's a Kotlin example in `Main.kt`:```kotlinfun sum(numbers: List<Int>): Int = numbers.sum()
```After:"""
        val expected =
            """Here's a Kotlin example in `Main.kt`:
```kotlin
fun sum(numbers: List<Int>): Int = numbers.sum()
```
After:"""
        assertEquals(expected, normalizeAgentFences(input))
    }

    @Test
    fun `does not treat prose after fence marker as opening fence`() {
        val input = "```A slightly richer version with data classes:"
        assertEquals(input, normalizeAgentFences(input))
    }

    @Test
    fun `splits citation fence when path merges with first code line`() {
        val input =
            """```3:10:src/main/kotlin/Person.ktclass Person(
 val name: String,
) {
}
```"""
        val expected =
            """```kotlin
class Person(
 val name: String,
) {
}
```"""
        assertEquals(expected, normalizeAgentFences(input))
    }

    @Test
    fun `normalization is idempotent on all fixtures`() {
        val dollar = "$"
        val fixtures =
            listOf(
                """intro

```python
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n - 1) + fibonacci(n - 2)
```

Example: `fibonacci(10)` → `55`.""",
                """```python
return fibonacci(n-1) + fibonacci(n-2)```Example: `fibonacci(10)` → `55`.""",
                """```python
return fibonacci(n-1) + fibonacci(n-2)
```Example: `fibonacci(10)` → `55`.""",
                """```kotlinfun summarize(numbers: List<Number>) {
 val total = numbers.sumOf { it.toDouble() }
 println(total)
}
```Example:""",
                "```kotlinsummarize(listOf(1,2,3,4.5)) // prints10.5```",
                """```kotlin
fun concat(a: String, b: String): String = ${dollar}a${dollar}b
""",
                "```kotlinfun concat(a: String, b: String): String = a + b```Or with string templates:",
                """Here's a Kotlin example in `Main.kt`:```kotlinfun sum(numbers: List<Int>): Int = numbers.sum()
```After:""",
                "```A slightly richer version with data classes:",
                """```3:10:src/main/kotlin/Person.ktclass Person(
 val name: String,
) {
}
```""",
                "```kotlin\r\nfun main()\r\n```\r\nAfter",
            )
        for (input in fixtures) {
            val once = normalizeAgentFences(input)
            assertEquals(once, normalizeAgentFences(once), "idempotent for: ${input.take(40)}…")
        }
    }

    @Test
    fun `accepts windows line endings and emits lf`() {
        val input = "```kotlin\r\nfun main()\r\n```\r\nAfter"
        val expected =
            """```kotlin
fun main()
```
After"""
        assertEquals(expected, normalizeAgentFences(input))
        assertTrue(!normalizeAgentFences(input).contains('\r'))
    }

    @Test
    fun `chunked streaming matches full buffer normalization`() {
        val chunks = listOf("```kotlinfun ", "sum(a: Int", ", b: Int)", "```After")
        val full = chunks.joinToString("")
        var accumulated = ""
        var lastResult = ""
        for (chunk in chunks) {
            accumulated += chunk
            lastResult = normalizeAgentFences(accumulated)
        }
        assertEquals(normalizeAgentFences(full), lastResult)
        val penultimate = chunks.dropLast(1).joinToString("")
        assertTrue(
            normalizeAgentFences(penultimate) != normalizeAgentFences(full),
            "partial buffer should differ until stream completes",
        )
    }
}
