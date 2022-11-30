package com.wutsi.checkout.manager.job

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchOrderRequest
import com.wutsi.checkout.manager.workflow.ExpireOrderWorkflow
import com.wutsi.platform.core.cron.AbstractCronJob
import com.wutsi.platform.core.cron.CronLockManager
import com.wutsi.platform.core.logging.DefaultKVLogger
import com.wutsi.workflow.WorkflowContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class ExpireOrderJob(
    private val checkoutAccessApi: CheckoutAccessApi,
    private val expireOrderWorkflow: ExpireOrderWorkflow,
    lockManager: CronLockManager,
) : AbstractCronJob(lockManager) {
    override fun getJobName() = "expire-order"

    @Scheduled(cron = "\${wutsi.application.jobs.pending-transaction.cron}")
    override fun run() {
        super.run()
    }

    override fun doRun(): Long {
        var count = 0L
        val limit = 100
        var offset = 0
        val now = OffsetDateTime.now()
        while (true) {
            val orders = checkoutAccessApi.searchOrder(
                request = SearchOrderRequest(
                    limit = limit,
                    offset = offset++,
                    expiredFrom = now
                )
            ).orders
            orders.forEach {
                if (expire(it.id)) {
                    count++
                }
            }

            if (orders.size < limit) {
                break
            }
        }
        return count
    }

    private fun expire(orderId: String): Boolean {
        val logger = DefaultKVLogger()
        logger.add("job", getJobName())
        logger.add("order_id", orderId)
        try {
            expireOrderWorkflow.execute(orderId, WorkflowContext())
            return true
        } catch (ex: Exception) {
            logger.setException(ex)
            return false
        } finally {
            logger.log()
        }
    }
}
