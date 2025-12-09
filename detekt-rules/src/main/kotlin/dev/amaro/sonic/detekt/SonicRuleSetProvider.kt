package dev.amaro.sonic.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * Detekt RuleSetProvider for Sonic framework custom rules.
 *
 * This provider registers all custom Detekt rules for the Sonic framework,
 * including the MutableMiddlewareRule that prevents race conditions in singleton
 * middleware implementations.
 *
 * The Detekt framework uses Java SPI (Service Provider Interface) to discover
 * RuleSetProvider implementations. The service configuration is specified in:
 * `META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider`
 */
class SonicRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "sonic-rules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                MutableMiddlewareRule(config)
            )
        )
    }
}
