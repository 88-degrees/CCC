package com.oztechan.ccc.client.core.shared.util

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter

actual fun Double.getFormatted(precision: Int) = NSNumberFormatter().apply {
    var currentPrecision = precision.toULong()
    setNumberStyle(NSNumberFormatterDecimalStyle)
    setGroupingSeparator(" ")
    setDecimalSeparator(".")
    setMaximumFractionDigits(currentPrecision)
    // increasing floating digits for too small numbers
    repeat(MAXIMUM_FLOATING_POINT - precision + 1) {
        if (stringFromNumber(NSNumber(this@getFormatted)).orEmpty() == "0") {
            currentPrecision = precision.toULong() + it.toULong()
            setMaximumFractionDigits(currentPrecision)
        }
    }
}.stringFromNumber(NSNumber(this)).orEmpty()
