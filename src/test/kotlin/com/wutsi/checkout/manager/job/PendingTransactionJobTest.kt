package com.wutsi.checkout.manager.job

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchTransactionResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.enums.TransactionType
import com.wutsi.platform.core.stream.EventStream
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PendingTransactionJobTest {
    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @MockBean
    private lateinit var eventStream: EventStream

    @Autowired
    protected lateinit var job: PendingTransactionJob

    @Test
    fun run() {
        // GIVEN
        val txs = listOf(
            Fixtures.createTransactionSummary("1", type = TransactionType.CHARGE),
            Fixtures.createTransactionSummary("1", type = TransactionType.CASHOUT)
        )
        doReturn(SearchTransactionResponse(txs)).whenever(checkoutAccessApi).searchTransaction(any())

        // WHEN
        job.run()

        // THEN
        verify(eventStream, times(2)).enqueue(
            eq(InternalEventURN.TRANSACTION_PENDING.urn),
            any()
        )
    }
}
