package com.pennypincher.pro.shared.infrastructure.web.resolver

/**
 * Marks a controller parameter of type `UserPrincipal` that must be filled with the authenticated
 * caller. Controllers use it instead of reading the security context themselves.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AuthenticatedUser
