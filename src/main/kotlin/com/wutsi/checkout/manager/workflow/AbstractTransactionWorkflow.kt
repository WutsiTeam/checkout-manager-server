package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.manager.event.TransactionEventPayload
import com.wutsi.error.ErrorURN
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.core.error.exception.ConflictException
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.WorkflowContext
import feign.FeignException
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractTransactionWorkflow<Req, Resp>(eventStream: EventStream) :
    AbstractCheckoutWorkflow<Req, Resp, TransactionEventPayload>(eventStream) {

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    override fun getEventType(request: Req, response: Resp, context: WorkflowContext): String? = null

    override fun toEventPayload(request: Req, response: Resp, context: WorkflowContext): TransactionEventPayload? = null

    protected fun handleChargeException(ex: FeignException): Throwable {
        val response = objectMapper.readValue(ex.contentUTF8(), ErrorResponse::class.java)
        return if (response.error.code == com.wutsi.checkout.access.error.ErrorURN.TRANSACTION_FAILED.urn) {
            ConflictException(
                error = response.error.copy(code = ErrorURN.TRANSACTION_FAILED.urn)
            )
        } else {
            ex
        }
    }
}
