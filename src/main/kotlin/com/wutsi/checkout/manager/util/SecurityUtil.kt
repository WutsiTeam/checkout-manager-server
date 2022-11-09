package com.wutsi.membership.manager.util

import com.wutsi.platform.core.security.SubjectType
import com.wutsi.platform.core.security.WutsiPrincipal
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtil {
    fun getAccountId(): Long {
        val principal = getWutsiPrincipal()
        return if (principal.type == SubjectType.USER) {
            principal.id.toLong()
        } else {
            throw IllegalStateException("The principal is not a user")
        }
    }

    private fun getWutsiPrincipal(): WutsiPrincipal =
        SecurityContextHolder.getContext()?.authentication?.principal as WutsiPrincipal
}
