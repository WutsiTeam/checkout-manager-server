package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchPaymentProviderRequest
import com.wutsi.checkout.access.enums.PaymentMethodType
import com.wutsi.checkout.manager.dto.AddPaymentMethodRequest
import com.wutsi.checkout.manager.workflow.AddPaymentMethodWorkflow
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.manager.event.MemberEventPayload
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
    private val workflow: AddPaymentMethodWorkflow
) {
    fun onMemberRegistered(event: Event) {
        val payload = toMemberPayload(event)
        log(payload)

        val account = membershipAccessApi.getAccount(payload.accountId).account
        val providers = checkoutAccessApi.searchPaymentProvider(
            SearchPaymentProviderRequest(
                country = account.phone.country,
                number = account.phone.number,
                type = PaymentMethodType.MOBILE_MONEY.name
            )
        ).paymentProviders

        if (providers.size == 1) {
            workflow.execute(
                WorkflowContext(
                    accountId = payload.accountId,
                    request = AddPaymentMethodRequest(
                        providerId = providers[0].id,
                        type = PaymentMethodType.MOBILE_MONEY.name,
                        number = account.phone.number,
                        country = account.phone.country,
                        ownerName = account.displayName
                    )
                )
            )
        }
    }

    private fun toMemberPayload(event: Event): MemberEventPayload =
        mapper.readValue(event.payload, MemberEventPayload::class.java)

    private fun log(payload: MemberEventPayload) {
        logger.add("payload_account_id", payload.accountId)
        logger.add("payload_phone_number", payload.phoneNumber)
        logger.add("payload_pin", "*********")
    }
}
