package com.wutsi.checkout.manager.job

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchOrderRequest
import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.cron.AbstractCronJob
import com.wutsi.platform.core.cron.CronLockManager
import com.wutsi.platform.core.stream.EventStream
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class ExpireOrderJob(
    private val checkoutAccessApi: CheckoutAccessApi,
    private val eventStream: EventStream,
    lockManager: CronLockManager
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
                eventStream.enqueue(
                    type = InternalEventURN.ORDER_EXPIRED.urn,
                    payload = OrderEventPayload(
                        orderId = it.id
                    )
                )
                count++
            }

            if (orders.size < limit) {
                break
            }
        }
        return count
    }
}
