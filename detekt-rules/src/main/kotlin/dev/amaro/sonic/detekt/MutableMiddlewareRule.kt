package dev.amaro.sonic.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry

/**
 * Detekt rule to detect mutable fields in @Singleton or @Single scoped IMiddleware implementations.
 *
 * Mutable instance fields in singleton middlewares cause race conditions because:
 * - Singleton instances are shared across all requests/operations
 * - Multiple threads may access the middleware concurrently
 * - Mutable fields can be modified by concurrent access
 *
 * This rule enforces strict singleton middleware design: only immutable fields allowed.
 *
 * Examples of violations:
 * ```
 * @Singleton
 * class ApiMiddleware : IMiddleware<State> {
 *     private var cache = mutableMapOf<String, Data>()  // <- VIOLATION
 *     private val state = mutableListOf<String>()       // <- VIOLATION
 * }
 * ```
 *
 * Allowed patterns:
 * ```
 * @Singleton
 * class LoggingMiddleware : IMiddleware<State> {
 *     // No mutable fields - OK
 *     override suspend fun process(...) { ... }
 * }
 *
 * @Factory
 * class StatefulMiddleware : IMiddleware<State> {
 *     private var cache = mutableMapOf<String, Data>()  // <- OK (Factory scoped)
 * }
 * ```
 */
class MutableMiddlewareRule(config: Config) : Rule(config) {
    override val issue = Issue(
        id = "MutableMiddlewareRule",
        severity = Severity.Warning,
        description = "Middleware with @Singleton or @Single scope must not have mutable fields. " +
                "Mutable fields in singleton middlewares cause race conditions when accessed concurrently.",
        debt = Debt.FIVE_MINS
    )

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)

        // Check if class implements IMiddleware<*>
        if (!implementsIMiddleware(classOrObject)) {
            return
        }

        // Check if class has @Singleton or @Single annotation
        if (!hasSingletonOrSingleAnnotation(classOrObject)) {
            return
        }

        // At this point: class is @Singleton/@Single IMiddleware
        // Find and report all mutable fields
        classOrObject.body?.properties?.forEach { property ->
            if (isMutableField(property)) {
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(classOrObject),
                        message = "Singleton middleware '${classOrObject.name}' has mutable field '${property.name}'. " +
                                "Mutable fields in singleton middlewares cause race conditions. " +
                                "Consider using @Factory scope or making the field immutable."
                    )
                )
            }
        }
    }

    /**
     * Check if a class implements IMiddleware interface.
     */
    private fun implementsIMiddleware(classOrObject: KtClassOrObject): Boolean {
        val superTypeList = classOrObject.superTypeListEntries
        return superTypeList.any { superType ->
            val typeText = when (superType) {
                is KtSuperTypeListEntry -> {
                    superType.typeReference?.text ?: ""
                }

                else -> ""
            }
            typeText.startsWith("IMiddleware")
        }
    }

    /**
     * Check if a class has @Singleton or @Single annotation.
     */
    private fun hasSingletonOrSingleAnnotation(annotated: KtAnnotated): Boolean {
        val annotations = annotated.annotationEntries.map { it.shortName?.asString() ?: "" }
        return "Singleton" in annotations || "Single" in annotations
    }

    /**
     * Check if a property is mutable.
     * A property is mutable if:
     * - It's declared with 'var' keyword (mutable variable)
     * - It's declared with 'val' but initialized with a mutable collection factory
     */
    private fun isMutableField(property: KtProperty): Boolean {
        // Check if declared with 'var' (mutable variable)
        if (property.isVar) {
            return true
        }

        // Check if initialized with mutable collection factory or type
        val initializer = property.initializer
        if (initializer != null) {
            return isMutableInitializer(initializer)
        }

        // Check the declared type for mutable types
        property.typeReference?.let { typeRef ->
            val typeText = typeRef.text
            if (isMutableType(typeText)) {
                return true
            }
        }

        return false
    }

    /**
     * Check if an initializer expression creates a mutable collection.
     */
    private fun isMutableInitializer(initializer: KtExpression): Boolean {
        val initializerText = initializer.text

        // Mutable factory functions
        val mutableFactories = setOf(
            "mutableListOf",
            "mutableMapOf",
            "mutableSetOf",
            "LinkedHashMap",
            "HashMap",
            "ArrayList",
            "HashSet",
            "LinkedHashSet",
            "ConcurrentHashMap",
            "Vector",
            "Stack"
        )

        // Check if initializer text contains any mutable factory function
        return mutableFactories.any { initializerText.contains(it) }
    }

    /**
     * Check if a type declaration is mutable.
     */
    private fun isMutableType(typeText: String): Boolean {
        val mutableTypes = setOf(
            "MutableList",
            "MutableMap",
            "MutableSet",
            "MutableCollection",
            "ConcurrentHashMap",
            "HashMap",
            "ArrayList",
            "HashSet",
            "LinkedHashSet",
            "Vector",
            "Stack"
        )

        // Check if type name contains any mutable type
        return mutableTypes.any { typeText.contains(it) }
    }
}
