package com.wutsi.checkout.manager.job

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchTransactionRequest
import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.checkout.manager.event.TransactionEventPayload
import com.wutsi.platform.core.cron.AbstractCronJob
import com.wutsi.platform.core.cron.CronLockManager
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.payment.core.Status
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PendingTransactionJob(
    private val checkoutAccessApi: CheckoutAccessApi,
    private val eventStream: EventStream,
    lockManager: CronLockManager
) : AbstractCronJob(lockManager) {
    override fun getJobName() = "pending-transaction"

    @Scheduled(cron = "\${wutsi.application.jobs.pending-transaction.cron}")
    override fun run() {
        super.run()
    }

    override fun doRun(): Long {
        var count = 0L
        val limit = 100
        var offset = 0
        while (true) {
            val txs = checkoutAccessApi.searchTransaction(
                request = SearchTransactionRequest(
                    status = listOf(Status.PENDING.name),
                    limit = limit,
                    offset = offset++
                )
            ).transactions
            txs.forEach {
                eventStream.enqueue(
                    type = InternalEventURN.TRANSACTION_PENDING.urn,
                    payload = TransactionEventPayload(
                        transactionId = it.id
                    )
                )
                count++
            }

            if (txs.size < limit) {
                break
            }
        }
        return count
    }
}
