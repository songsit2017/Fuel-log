package com.songsit.fuellogpro.data.firebase

internal fun readFuelPaymentMethod(value: Any?): String = (value as? String).orEmpty()

internal fun canEnrichPayment(
    local: Map<String, Any?>,
    remote: Map<String, Any?>,
    remoteHasPaymentField: Boolean,
): Boolean = !remoteHasPaymentField &&
    readFuelPaymentMethod(local["paymentMethod"]).isNotBlank() &&
    local.minus("paymentMethod") == remote.minus("paymentMethod")
