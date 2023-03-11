package com.wutsi.checkout.manager.webhook

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.wutsi.checkout.manager.workflow.task.ProcessPendingTransactionTask
import com.wutsi.platform.payment.provider.flutterwave.model.FWResponseData
import com.wutsi.platform.payment.provider.flutterwave.model.FWWebhookRequest
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.WorkflowEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class FWWebhookControllerTest : ClientHttpRequestInterceptor {
    @LocalServerPort
    public val port: Int = 0

    @MockBean
    private lateinit var workflowEngine: WorkflowEngine

    @Value("\${wutsi.flutterwave.secret-hash}")
    private lateinit var secretHash: String

    private val rest = RestTemplate()

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        request.headers.add("verif-hash", secretHash)
        return execution.execute(request, body)
    }

    @BeforeEach
    fun setUp() {
        rest.interceptors.add(this)
    }

    @Test
    fun processEvent() {
        val request = FWWebhookRequest(
            event = "payment.completed",
            data = FWResponseData(
                id = 1203920932,
                tx_ref = "-transaction-id-",
            ),
        )
        rest.postForEntity(url(), request, Any::class.java)

        verify(workflowEngine).executeAsync(
            ProcessPendingTransactionTask.ID,
            WorkflowContext(
                data = mutableMapOf(
                    ProcessPendingTransactionTask.CONTEXT_TRANSACTION_ID to request.data.tx_ref!!,
                ),
            ),
        )
    }

    @Test
    fun invalidHash() {
        val request = FWWebhookRequest(
            event = "payment.completed",
            FWResponseData(
                id = 1203920932,
                tx_ref = "-transaction-id-",
            ),
        )
        RestTemplate().postForEntity(url(), request, Any::class.java)

        verify(workflowEngine, never()).executeAsync(any(), any())
    }

    @Test
    fun invalidEvent() {
        val request = FWWebhookRequest(
            event = "payment.completed",
            FWResponseData(
                id = 1203920932,
                tx_ref = null,
            ),
        )
        rest.postForEntity(url(), request, Any::class.java)

        verify(workflowEngine, never()).executeAsync(any(), any())
    }

    private fun url() = "http://localhost:$port/flutterwave/webhook"
}
