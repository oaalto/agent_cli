package com.oaalto.agent.acp.plan

import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.acp.transcript.model.PlanEntry
import com.oaalto.agent.acp.transcript.model.PlanEntryPriority
import com.oaalto.agent.acp.transcript.model.PlanEntryStatus
import com.oaalto.agent.acp.transcript.model.PlanVariant
import com.oaalto.agent.acp.transcript.model.StructuredUpdate

/**
 * Maps ACP plan-related SessionUpdate events to normalized [StructuredUpdate] values.
 *
 * Note: PlanUpdate, PlanUpdateV2, and PlanRemoved are marked @UnstableApi in the
 * ACP library and may not be available at runtime. This mapper uses reflection
 * to gracefully handle missing classes or properties.
 *
 * This reflection-based approach is temporary until the ACP SDK stabilizes
 * the plan API (PlanUpdate, PlanUpdateV2, PlanRemoved are currently marked @UnstableApi).
 * Once the API stabilizes, this should be converted to direct method calls.
 */
internal object PlanUpdateMapper {
    private val log = AgentCliLog.getInstance(PlanUpdateMapper::class.java)

    /**
     * Attempts to map a PlanUpdate to StructuredUpdate.
     * Returns null if the class or required properties are unavailable.
     */
    fun mapPlanUpdate(update: Any): StructuredUpdate.StartOrUpdatePlan? {
        return try {
            val planId =
                update::class.java.getMethod("getPlanId").invoke(update) as? String
                    ?: return null
            val entriesList =
                update::class.java.getMethod("getEntries").invoke(update) as? List<*>
                    ?: return null

            val entries =
                entriesList.mapNotNull { entry ->
                    mapPlanEntry(entry ?: return@mapNotNull null)
                }

            StructuredUpdate.StartOrUpdatePlan(
                planId = planId,
                entries = entries,
                variant = PlanVariant.Items,
            )
        } catch (e: NoSuchMethodException) {
            null // Expected when SDK method doesn't exist - gracefully degrade
        } catch (e: ClassNotFoundException) {
            null // Expected when SDK class not available - gracefully degrade
        } catch (e: ReflectiveOperationException) {
            log.debug("Reflection failed in mapPlanUpdate", throwable = e)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("Reflection failed in mapPlanUpdate", throwable = e)
            null
        }
    }

    /**
     * Attempts to map a PlanUpdateV2 to StructuredUpdate.
     * Returns null if the class or required properties are unavailable.
     */
    fun mapPlanUpdateV2(update: Any): StructuredUpdate.StartOrUpdatePlan? {
        return try {
            val planId =
                update::class.java.getMethod("getPlanId").invoke(update) as? String
                    ?: return null
            val plan =
                update::class.java.getMethod("getPlan").invoke(update)
                    ?: return null

            mapPlanVariant(planId, plan)
        } catch (e: NoSuchMethodException) {
            null // Expected when SDK method doesn't exist - gracefully degrade
        } catch (e: ClassNotFoundException) {
            null // Expected when SDK class not available - gracefully degrade
        } catch (e: ReflectiveOperationException) {
            log.debug("Reflection failed in mapPlanUpdateV2", throwable = e)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("Reflection failed in mapPlanUpdateV2", throwable = e)
            null
        }
    }

    /**
     * Attempts to map a PlanRemoved to StructuredUpdate.
     * Returns null if the class or required properties are unavailable.
     */
    fun mapPlanRemoved(update: Any): StructuredUpdate.RemovePlan? {
        return try {
            val planId =
                update::class.java.getMethod("getPlanId").invoke(update) as? String
                    ?: return null
            StructuredUpdate.RemovePlan(planId = planId)
        } catch (e: NoSuchMethodException) {
            null // Expected when SDK method doesn't exist - gracefully degrade
        } catch (e: ClassNotFoundException) {
            null // Expected when SDK class not available - gracefully degrade
        } catch (e: ReflectiveOperationException) {
            log.debug("Reflection failed in mapPlanRemoved", throwable = e)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("Reflection failed in mapPlanRemoved", throwable = e)
            null
        }
    }

    private fun mapPlanVariant(
        planId: String,
        plan: Any,
    ): StructuredUpdate.StartOrUpdatePlan? =
        when (plan::class.java.simpleName) {
            "Items" -> mapItemsVariant(planId, plan)
            "File" -> mapFileVariant(planId, plan)
            "Markdown" -> mapMarkdownVariant(planId, plan)
            else -> null
        }

    private fun mapItemsVariant(
        planId: String,
        variant: Any,
    ): StructuredUpdate.StartOrUpdatePlan? {
        return try {
            val entriesList =
                variant::class.java.getMethod("getEntries").invoke(variant) as? List<*>
                    ?: return null

            val entries =
                entriesList.mapNotNull { entry ->
                    mapPlanEntry(entry ?: return@mapNotNull null)
                }

            StructuredUpdate.StartOrUpdatePlan(
                planId = planId,
                entries = entries,
                variant = PlanVariant.Items,
            )
        } catch (e: NoSuchMethodException) {
            null // Expected when SDK method doesn't exist - gracefully degrade
        } catch (e: ClassNotFoundException) {
            null // Expected when SDK class not available - gracefully degrade
        } catch (e: ReflectiveOperationException) {
            log.debug("Reflection failed in mapItemsVariant", throwable = e)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("Reflection failed in mapItemsVariant", throwable = e)
            null
        }
    }

    private fun mapFileVariant(
        planId: String,
        variant: Any,
    ): StructuredUpdate.StartOrUpdatePlan? {
        return try {
            val uri =
                variant::class.java.getMethod("getUri").invoke(variant) as? String
                    ?: return null
            StructuredUpdate.StartOrUpdatePlan(
                planId = planId,
                entries = emptyList(),
                variant = PlanVariant.File(uri),
            )
        } catch (e: NoSuchMethodException) {
            null // Expected when SDK method doesn't exist - gracefully degrade
        } catch (e: ClassNotFoundException) {
            null // Expected when SDK class not available - gracefully degrade
        } catch (e: ReflectiveOperationException) {
            log.debug("Reflection failed in mapFileVariant", throwable = e)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("Reflection failed in mapFileVariant", throwable = e)
            null
        }
    }

    private fun mapMarkdownVariant(
        planId: String,
        variant: Any,
    ): StructuredUpdate.StartOrUpdatePlan? {
        return try {
            val content =
                variant::class.java.getMethod("getMarkdown").invoke(variant) as? String
                    ?: return null
            StructuredUpdate.StartOrUpdatePlan(
                planId = planId,
                entries = emptyList(),
                variant = PlanVariant.Markdown(content),
            )
        } catch (e: NoSuchMethodException) {
            null // Expected when SDK method doesn't exist - gracefully degrade
        } catch (e: ClassNotFoundException) {
            null // Expected when SDK class not available - gracefully degrade
        } catch (e: ReflectiveOperationException) {
            log.debug("Reflection failed in mapMarkdownVariant", throwable = e)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("Reflection failed in mapMarkdownVariant", throwable = e)
            null
        }
    }

    private fun mapPlanEntry(entry: Any): PlanEntry? =
        try {
            val content = entry::class.java.getMethod("getContent").invoke(entry) as? String
            val status = entry::class.java.getMethod("getStatus").invoke(entry)
            val priority = entry::class.java.getMethod("getPriority").invoke(entry)

            if (content == null || status == null || priority == null) {
                null
            } else {
                PlanEntry(
                    content = content,
                    status = mapPlanEntryStatus(status),
                    priority = mapPlanEntryPriority(priority),
                )
            }
        } catch (e: NoSuchMethodException) {
            null // Expected when SDK method doesn't exist - gracefully degrade
        } catch (e: ClassNotFoundException) {
            null // Expected when SDK class not available - gracefully degrade
        } catch (e: ReflectiveOperationException) {
            log.debug("Reflection failed in mapPlanEntry", throwable = e)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("Reflection failed in mapPlanEntry", throwable = e)
            null
        }

    private fun mapPlanEntryStatus(status: Any): PlanEntryStatus =
        when (status::class.java.simpleName) {
            "PENDING" -> PlanEntryStatus.PENDING
            "IN_PROGRESS" -> PlanEntryStatus.IN_PROGRESS
            "COMPLETED" -> PlanEntryStatus.COMPLETED
            else -> PlanEntryStatus.PENDING
        }

    private fun mapPlanEntryPriority(priority: Any): PlanEntryPriority =
        when (priority::class.java.simpleName) {
            "HIGH" -> PlanEntryPriority.HIGH
            "MEDIUM" -> PlanEntryPriority.MEDIUM
            "LOW" -> PlanEntryPriority.LOW
            else -> PlanEntryPriority.MEDIUM
        }
}
