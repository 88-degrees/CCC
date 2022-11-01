package com.oztechan.ccc.config.mapper

import com.oztechan.ccc.config.service.ad.AdConfigEntity
import com.oztechan.ccc.test.BaseTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AdConfigTest : BaseTest() {

    @Test
    fun toModel() {
        val entity = AdConfigEntity()
        val model = entity.toModel()

        assertEquals(entity.bannerAdSessionCount, model.bannerAdSessionCount)
        assertEquals(entity.interstitialAdSessionCount, model.interstitialAdSessionCount)
        assertEquals(entity.interstitialAdInitialDelay, model.interstitialAdInitialDelay)
        assertEquals(entity.interstitialAdPeriod, model.interstitialAdPeriod)
    }
}
