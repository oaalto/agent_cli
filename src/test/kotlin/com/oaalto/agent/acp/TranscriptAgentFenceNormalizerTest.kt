package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
