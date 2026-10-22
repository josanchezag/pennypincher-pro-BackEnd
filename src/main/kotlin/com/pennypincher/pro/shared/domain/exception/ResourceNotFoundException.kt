package com.pennypincher.pro.shared.domain.exception

/** The resource does not exist, is inactive or belongs to another user; the three cases look the same. */
class ResourceNotFoundException(resourceName: String) :
    BusinessException(ErrorCode.NOT_FOUND, "$resourceName not found")
