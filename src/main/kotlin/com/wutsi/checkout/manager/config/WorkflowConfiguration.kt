package com.wutsi.checkout.manager.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.engine.WorkflowEventPayload
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

@Configuration
class WorkflowConfiguration(
    private val eventStream: EventStream,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun workflowEngine(): WorkflowEngine =
        WorkflowEngine(eventStream)

    @EventListener
    fun handleEvent(event: Event) {
        when (event.type) {
            WorkflowEngine.EVENT_EXECUTE -> {
                val payload = objectMapper.readValue(event.payload, WorkflowEventPayload::class.java)
                workflowEngine().execute(payload.id, payload.context)
            }
            else -> {}
        }
    }
}
