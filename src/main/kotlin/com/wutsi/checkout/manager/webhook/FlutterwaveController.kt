package com.wutsi.checkout.manager.webhook

import com.wutsi.checkout.manager.workflow.task.ProcessPendingTransactionTask
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.provider.flutterwave.model.FWWebhookRequest
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.WorkflowEngine
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * See https://developer.flutterwave.com/docs/integration-guides/webhooks
 */
@RestController
@RequestMapping("/flutterwave")
public class FlutterwaveController(
    private val logger: KVLogger,
    private val workflowEngine: WorkflowEngine,
    @Value("\${wutsi.flutterwave.secret-hash}") private val secretHash: String,
) {
    @PostMapping("/webhook")
    public fun invoke(
        @RequestBody request: FWWebhookRequest,
        @RequestHeader(name = "verif-hash", required = false) verifHash: String? = null,
    ) {
        log(request)

        // Verify the hash
        if (secretHash != verifHash) {
            logger.add("hash-valid", false)
            return // This is not coming from Flutterwave - silently ignore it
        }
        logger.add("hash-valid", true)

        // Handle the request
        handleNotification(request)
    }

    private fun handleNotification(request: FWWebhookRequest) {
        val transactionId = request.data.tx_ref
        if (transactionId != null) {
            workflowEngine.executeAsync(
                ProcessPendingTransactionTask.ID,
                WorkflowContext(
                    data = mutableMapOf(ProcessPendingTransactionTask.CONTEXT_TRANSACTION_ID to transactionId),
                ),
            )
        }
    }

    private fun log(request: FWWebhookRequest) {
        logger.add("request_event", request.event)
        logger.add("request_data_id", request.data.id)
        logger.add("request_data_status", request.data.status)
        logger.add("request_data_flw_ref", request.data.flw_ref)
        logger.add("request_data_tx_ref", request.data.tx_ref)
        logger.add("request_data_app_fee", request.data.app_fee)
        logger.add("request_data_fee", request.data.fee)
        logger.add("request_data_amount", request.data.amount)
        logger.add("request_data_currency", request.data.currency)
    }
}
