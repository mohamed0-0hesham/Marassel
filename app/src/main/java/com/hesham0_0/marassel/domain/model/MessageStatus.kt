package com.hesham0_0.marassel.domain.model

enum class MessageStatus {
    PENDING,
    SENT,
    FAILED,
}

val MessageStatus.isTerminal: Boolean
    get() = this == MessageStatus.SENT || this == MessageStatus.FAILED


val MessageStatus.isRetryable: Boolean
    get() = this == MessageStatus.FAILED