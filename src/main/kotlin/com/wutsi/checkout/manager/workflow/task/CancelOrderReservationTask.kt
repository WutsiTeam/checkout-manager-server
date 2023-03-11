package com.wutsi.checkout.manager.workflow.task

import com.wutsi.enums.ReservationStatus
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.marketplace.access.dto.SearchReservationRequest
import com.wutsi.marketplace.access.dto.UpdateReservationStatusRequest
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class CancelOrderReservationTask(
    private val marketplaceAccessApi: MarketplaceAccessApi,
    private val workflowEngine: WorkflowEngine,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "cancel-order-reservation")
        const val CONTEXT_ORDER_ID = "order-id"
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    override fun execute(context: WorkflowContext) {
        marketplaceAccessApi.searchReservation(
            SearchReservationRequest(
                orderId = context.data[CONTEXT_ORDER_ID] as String,
            ),
        ).reservations.forEach {
            marketplaceAccessApi.updateReservationStatus(
                id = it.id,
                request = UpdateReservationStatusRequest(
                    status = ReservationStatus.CANCELLED.name,
                ),
            )
        }
    }
}
