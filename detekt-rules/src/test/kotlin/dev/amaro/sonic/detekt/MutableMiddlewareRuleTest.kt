package dev.amaro.sonic.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MutableMiddlewareRuleTest {

    private val rule = MutableMiddlewareRule(Config.empty)

    @Test
    fun `detects mutable var field in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class ApiMiddleware : IMiddleware<String> {
                private var cache = mutableMapOf<String, String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("mutable field 'cache'"))
    }

    @Test
    fun `detects mutable val with mutableListOf in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class DataMiddleware : IMiddleware<String> {
                private val items = mutableListOf<String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("mutable field 'items'"))
    }

    @Test
    fun `detects multiple mutable fields in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class ComplexMiddleware : IMiddleware<String> {
                private var cache = mutableMapOf<String, String>()
                private val list = mutableListOf<Int>()
                private var state = ArrayList<String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(3, findings.size)
    }

    @Test
    fun `detects mutable HashMap in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class CacheMiddleware : IMiddleware<String> {
                private val cache: HashMap<String, String> = HashMap()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `detects mutable ArrayList in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class ListMiddleware : IMiddleware<String> {
                private val items = ArrayList<String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `ignores immutable singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class LoggingMiddleware : IMiddleware<String> {
                private val logger = object {}

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `ignores mutable fields in non-singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Factory

            @Factory
            class StatefulMiddleware : IMiddleware<String> {
                private var cache = mutableMapOf<String, String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `ignores mutable fields in non-middleware classes`() {
        val code = """
            annotation class Singleton

            @Singleton
            class RegularClass {
                private var cache = mutableMapOf<String, String>()
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `detects mutable Single annotation from Koin`() {
        val code = """
            interface IMiddleware<T>

            annotation class Single

            @Single
            class KoinMiddleware : IMiddleware<String> {
                private val state = mutableListOf<String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("mutable field 'state'"))
    }

    @Test
    fun `detects mutable MutableSet in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class SetMiddleware : IMiddleware<String> {
                private val tags = mutableSetOf<String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `detects mutable HashSet in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class SetMiddleware : IMiddleware<String> {
                private val tags: HashSet<String> = HashSet()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `detects var with immutable type in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class VersionMiddleware : IMiddleware<String> {
                private var version: String = "1.0"

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("mutable field 'version'"))
    }

    @Test
    fun `detects mutable ConcurrentHashMap in singleton middleware`() {
        val code = """
            import java.util.concurrent.ConcurrentHashMap

            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class ThreadSafeMiddleware : IMiddleware<String> {
                private val cache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `ignores singleton middleware with only final immutable fields`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class ImmutableMiddleware : IMiddleware<String> {
                private val config: Map<String, String> = emptyMap()
                private val version: String = "1.0"
                private val enabled: Boolean = true

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `detects Vector in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class VectorMiddleware : IMiddleware<String> {
                private val items = Vector<String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `detects Stack in singleton middleware`() {
        val code = """
            interface IMiddleware<T>

            annotation class Singleton

            @Singleton
            class StackMiddleware : IMiddleware<String> {
                private val operations = Stack<String>()

                suspend fun process() {}
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }
}
