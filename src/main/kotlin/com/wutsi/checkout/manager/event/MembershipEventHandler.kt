package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchPaymentMethodRequest
import com.wutsi.checkout.access.dto.SearchPaymentProviderRequest
import com.wutsi.checkout.manager.dto.AddPaymentMethodRequest
import com.wutsi.checkout.manager.workflow.AddPaymentMethodWorkflow
import com.wutsi.checkout.manager.workflow.CreateBusinessWorkflow
import com.wutsi.checkout.manager.workflow.DeactivateBusinessWorkflow
import com.wutsi.checkout.manager.workflow.DeactivatePaymentMethodWorkflow
import com.wutsi.enums.PaymentMethodStatus
import com.wutsi.enums.PaymentMethodType
import com.wutsi.event.MemberEventPayload
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class MembershipEventHandler(
    private val mapper: ObjectMapper,
    private val logger: KVLogger,
    private val membershipAccessApi: MembershipAccessApi,
    private val checkoutAccessApi: CheckoutAccessApi,
    private val addPaymentMethodWorkflow: AddPaymentMethodWorkflow,
    private val createBusinessWorkflow: CreateBusinessWorkflow,
    private val deactivateBusinessWorkflow: DeactivateBusinessWorkflow,
    private val deactivatePaymentMethodWorkflow: DeactivatePaymentMethodWorkflow
) {
    fun onMemberRegistered(event: Event) {
        val payload = toMemberPayload(event)
        log(payload)

        val account = membershipAccessApi.getAccount(payload.accountId).account
        val type = PaymentMethodType.MOBILE_MONEY
        val providers = checkoutAccessApi.searchPaymentProvider(
            SearchPaymentProviderRequest(
                country = account.phone.country,
                number = account.phone.number,
                type = type.name
            )
        ).paymentProviders

        if (providers.size == 1) {
            val request = AddPaymentMethodRequest(
                providerId = providers[0].id,
                type = type.name,
                number = account.phone.number,
                country = account.phone.country,
                ownerName = account.displayName
            )
            val context = WorkflowContext(accountId = payload.accountId)
            val response = addPaymentMethodWorkflow.execute(request, context)
            logger.add("payment_method_token", response.paymentMethodToken)
        }
    }

    fun onMemberDeleted(event: Event) {
        val payload = toMemberPayload(event)
        log(payload)

        val context = WorkflowContext(accountId = payload.accountId)
        deactivateBusinessWorkflow.execute(null, context)
        checkoutAccessApi.searchPaymentMethod(
            request = SearchPaymentMethodRequest(
                accountId = payload.accountId,
                status = PaymentMethodStatus.ACTIVE.name
            )
        ).paymentMethods.forEach {
            deactivatePaymentMethodWorkflow.execute(it.token, context)
        }
    }

    fun onBusinessActivated(event: Event) {
        val payload = toMemberPayload(event)
        log(payload)

        val context = WorkflowContext(accountId = payload.accountId)
        val businessId = createBusinessWorkflow.execute(null, context)
        logger.add("business_id", businessId)
    }

    fun onBusinesstDeactivated(event: Event) {
        val payload = toMemberPayload(event)
        log(payload)

        val context = WorkflowContext(accountId = payload.accountId)
        val businessId = deactivateBusinessWorkflow.execute(null, context)
        logger.add("business_id", businessId)
    }

    private fun toMemberPayload(event: Event): MemberEventPayload =
        mapper.readValue(event.payload, MemberEventPayload::class.java)

    private fun log(payload: MemberEventPayload) {
        logger.add("payload_account_id", payload.accountId)
        logger.add("payload_phone_number", payload.phoneNumber)
        logger.add("payload_pin", "*********")
    }
}
