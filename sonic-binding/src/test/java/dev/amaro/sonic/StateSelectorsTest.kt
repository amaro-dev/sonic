package dev.amaro.sonic

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for StateFlow.selectDistinct() extension function.
 *
 * Tests verify:
 * - Selector function is applied to each state emission
 * - distinctUntilChanged prevents emissions when selected value equals previous value
 * - Works with data classes (structural equality)
 * - Works with collections (element-by-element comparison)
 * - Multiple selectDistinct calls can be chained
 */
class StateSelectorsTest {

    data class TestState(
        val count: Int,
        val name: String,
        val items: List<String> = emptyList(),
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * Test: selectDistinct() applies selector function to each state emission
     */
    @Test
    fun `selectDistinct transforms state value`() = runTest {
        val stateFlow = MutableStateFlow(TestState(count = 5, name = "test"))
        val emissions = mutableListOf<Int>()

        val selected = stateFlow.selectDistinct { it.count }

        val job = launch {
            selected.collect { emissions.add(it) }
        }
        advanceUntilIdle()

        // Initial emission
        assertEquals(listOf(5), emissions)

        // Update state with different count
        stateFlow.value = TestState(count = 10, name = "test")
        advanceUntilIdle()
        assertEquals(listOf(5, 10), emissions)

        // Update state with different name but same count
        stateFlow.value = TestState(count = 10, name = "updated")
        advanceUntilIdle()
        assertEquals(listOf(5, 10), emissions)  // No new emission for count

        job.cancel()
    }

    /**
     * Test: selectDistinct emits only when selected value changes (distinctUntilChanged)
     */
    @Test
    fun `selectDistinct emits only when selected value changes`() = runTest {
        val stateFlow = MutableStateFlow(TestState(count = 5, name = "original"))
        val emissions = mutableListOf<String>()

        val selected = stateFlow.selectDistinct { it.name }

        val job = launch {
            selected.collect { emissions.add(it) }
        }
        advanceUntilIdle()

        // Initial emission
        assertEquals(listOf("original"), emissions)

        // Change name - SHOULD EMIT
        stateFlow.value = TestState(count = 5, name = "updated")
        advanceUntilIdle()
        assertEquals(listOf("original", "updated"), emissions)

        // Change count but keep name - SHOULD NOT EMIT
        stateFlow.value = TestState(count = 10, name = "updated")
        advanceUntilIdle()
        assertEquals(listOf("original", "updated"), emissions)

        // Change name again - SHOULD EMIT
        stateFlow.value = TestState(count = 10, name = "changed")
        advanceUntilIdle()
        assertEquals(listOf("original", "updated", "changed"), emissions)

        job.cancel()
    }

    /**
     * Test: selectDistinct works with data classes (structural equality)
     *
     * Data classes compare by fields, so two instances with same field values are equal.
     * This prevents unnecessary emissions when transformed data hasn't actually changed.
     */
    @Test
    fun `selectDistinct works with data classes using structural equality`() = runTest {
        data class Stats(val total: Int, val open: Int)

        val stateFlow = MutableStateFlow(TestState(count = 5, name = "test", items = listOf("a", "b")))
        val emissions = mutableListOf<Stats>()

        val selected = stateFlow.selectDistinct { state ->
            Stats(total = state.items.size, open = state.count)
        }

        val job = launch {
            selected.collect { emissions.add(it) }
        }
        advanceUntilIdle()

        // Initial: 2 items, count 5
        assertEquals(listOf(Stats(2, 5)), emissions)

        // Change name (items and count same) - NO EMISSION
        stateFlow.value = TestState(count = 5, name = "updated", items = listOf("a", "b"))
        advanceUntilIdle()
        assertEquals(listOf(Stats(2, 5)), emissions)

        // Change count - EMITS new Stats
        stateFlow.value = TestState(count = 3, name = "updated", items = listOf("a", "b"))
        advanceUntilIdle()
        assertEquals(listOf(Stats(2, 5), Stats(2, 3)), emissions)

        // Same stats again - NO EMISSION
        stateFlow.value = TestState(count = 3, name = "different", items = listOf("a", "b"))
        advanceUntilIdle()
        assertEquals(listOf(Stats(2, 5), Stats(2, 3)), emissions)

        job.cancel()
    }

    /**
     * Test: selectDistinct works with filtered collections
     *
     * Collections compare by elements, so different lists don't reuse previous value.
     * This test verifies filtering use case.
     */
    @Test
    fun `selectDistinct filters collections correctly`() = runTest {
        val stateFlow = MutableStateFlow(
            TestState(
                count = 5,
                name = "test",
                items = listOf("apple", "banana", "avocado")
            )
        )
        val emissions = mutableListOf<List<String>>()

        // Select only items starting with 'a'
        val selected = stateFlow.selectDistinct { state ->
            state.items.filter { it.startsWith("a") }
        }

        val job = launch {
            selected.collect { emissions.add(it) }
        }
        advanceUntilIdle()

        // Initial: ["apple", "avocado"]
        assertEquals(listOf(listOf("apple", "avocado")), emissions)

        // Change count but items stay same - NO EMISSION (same filtered list)
        stateFlow.value = TestState(count = 10, name = "test", items = listOf("apple", "banana", "avocado"))
        advanceUntilIdle()
        assertEquals(listOf(listOf("apple", "avocado")), emissions)

        // Add new 'a' item - EMITS new list
        stateFlow.value = TestState(count = 10, name = "test", items = listOf("apple", "banana", "avocado", "apricot"))
        advanceUntilIdle()
        assertEquals(listOf(listOf("apple", "avocado"), listOf("apple", "avocado", "apricot")), emissions)

        // Remove non-'a' item (filtered list same) - NO EMISSION
        stateFlow.value = TestState(count = 10, name = "test", items = listOf("apple", "avocado", "apricot"))
        advanceUntilIdle()
        assertEquals(listOf(listOf("apple", "avocado"), listOf("apple", "avocado", "apricot")), emissions)

        job.cancel()
    }

    /**
     * Test: Multiple selectDistinct calls can be chained
     *
     * Verifies that selectDistinct returns a Flow that can be further transformed
     * with additional selectDistinct calls.
     */
    @Test
    fun `selectDistinct can be chained for multiple transformations`() = runTest {
        val stateFlow = MutableStateFlow(TestState(count = 10, name = "test", items = listOf("a", "b", "c")))
        val emissions = mutableListOf<Int>()

        // Chain 1: Select items, then select count
        val selected = stateFlow
            .selectDistinct { it.items }
            .selectDistinct { items -> items.size }

        val job = launch {
            selected.collect { emissions.add(it) }
        }
        advanceUntilIdle()

        // Initial: 3 items
        assertEquals(listOf(3), emissions)

        // Change count (items same) - NO EMISSION
        stateFlow.value = TestState(count = 20, name = "test", items = listOf("a", "b", "c"))
        advanceUntilIdle()
        assertEquals(listOf(3), emissions)

        // Add item - EMITS new count
        stateFlow.value = TestState(count = 20, name = "test", items = listOf("a", "b", "c", "d"))
        advanceUntilIdle()
        assertEquals(listOf(3, 4), emissions)

        // Add another item - EMITS new count
        stateFlow.value = TestState(count = 20, name = "test", items = listOf("a", "b", "c", "d", "e"))
        advanceUntilIdle()
        assertEquals(listOf(3, 4, 5), emissions)

        job.cancel()
    }

    /**
     * Test: selectDistinct handles complex transformations (filtering + mapping)
     */
    @Test
    fun `selectDistinct works with complex filtering and mapping`() = runTest {
        data class ItemSummary(val count: Int, val names: List<String>)

        val stateFlow = MutableStateFlow(
            TestState(
                count = 5,
                name = "test",
                items = listOf("apple", "banana", "avocado", "cherry")
            )
        )
        val emissions = mutableListOf<ItemSummary>()

        // Complex selector: filter items, count them, extract names
        val selected = stateFlow.selectDistinct { state ->
            val filtered = state.items.filter { it.length > 5 }
            ItemSummary(count = filtered.size, names = filtered)
        }

        val job = launch {
            selected.collect { emissions.add(it) }
        }
        advanceUntilIdle()

        // Initial: ["banana", "avocado", "cherry"] (3 items with length > 5)
        assertEquals(listOf(ItemSummary(3, listOf("banana", "avocado", "cherry"))), emissions)

        // Change count (items same) - NO EMISSION
        stateFlow.value = TestState(
            count = 10,
            name = "test",
            items = listOf("apple", "banana", "avocado", "cherry")
        )
        advanceUntilIdle()
        assertEquals(listOf(ItemSummary(3, listOf("banana", "avocado", "cherry"))), emissions)

        // Add short item (filtered list same) - NO EMISSION
        stateFlow.value = TestState(
            count = 10,
            name = "test",
            items = listOf("apple", "banana", "avocado", "cherry", "fig")
        )
        advanceUntilIdle()
        assertEquals(listOf(ItemSummary(3, listOf("banana", "avocado", "cherry"))), emissions)

        // Add long item (filtered list changes) - EMITS
        stateFlow.value = TestState(
            count = 10,
            name = "test",
            items = listOf("apple", "banana", "avocado", "cherry", "blueberry")
        )
        advanceUntilIdle()
        assertEquals(
            listOf(
                ItemSummary(3, listOf("banana", "avocado", "cherry")),
                ItemSummary(4, listOf("banana", "avocado", "cherry", "blueberry"))
            ), emissions
        )

        job.cancel()
    }

    /**
     * Test: selectDistinct with empty collections
     */
    @Test
    fun `selectDistinct handles empty collections`() = runTest {
        val stateFlow = MutableStateFlow(TestState(count = 0, name = "test", items = emptyList()))
        val emissions = mutableListOf<List<String>>()

        val selected = stateFlow.selectDistinct { it.items }

        val job = launch {
            selected.collect { emissions.add(it) }
        }
        advanceUntilIdle()

        // Initial: empty list
        assertEquals(listOf<List<String>>(emptyList()), emissions)

        // Add items - EMITS
        stateFlow.value = TestState(count = 2, name = "test", items = listOf("a", "b"))
        advanceUntilIdle()
        assertEquals(listOf<List<String>>(emptyList(), listOf("a", "b")), emissions)

        // Back to empty - EMITS
        stateFlow.value = TestState(count = 0, name = "test", items = emptyList())
        advanceUntilIdle()
        assertEquals(listOf<List<String>>(emptyList(), listOf("a", "b"), emptyList()), emissions)

        job.cancel()
    }
}
