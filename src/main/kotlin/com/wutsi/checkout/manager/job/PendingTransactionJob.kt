package com.wutsi.checkout.manager.job

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchTransactionRequest
import com.wutsi.checkout.access.dto.TransactionSummary
import com.wutsi.checkout.manager.workflow.ProcessPendingTransactionWorkflow
import com.wutsi.platform.core.cron.AbstractCronJob
import com.wutsi.platform.core.cron.CronLockManager
import com.wutsi.platform.core.logging.DefaultKVLogger
import com.wutsi.platform.payment.core.Status
import com.wutsi.workflow.WorkflowContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PendingTransactionJob(
    private val checkoutAccessApi: CheckoutAccessApi,
    private val workflow: ProcessPendingTransactionWorkflow,
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
                if (sync(it)) {
                    count++
                }
            }

            if (txs.size < limit) {
                break
            }
        }
        return count
    }

    private fun sync(tx: TransactionSummary): Boolean {
        val logger = DefaultKVLogger()
        logger.add("job", getJobName())
        logger.add("transaction_status", tx.id)
        try {
            workflow.execute(tx.id, WorkflowContext())
            return true
        } catch (ex: Exception) {
            logger.setException(ex)
            return false
        } finally {
            logger.log()
        }
    }
}
