package com.oztechan.ccc.client.repository.adcontrol

import com.oztechan.ccc.client.configservice.ad.AdConfigService
import com.oztechan.ccc.client.core.shared.util.isPassed
import com.oztechan.ccc.client.storage.app.AppStorage

internal class AdControlRepositoryImpl(
    private val appStorage: AppStorage,
    private val adConfigService: AdConfigService
) : AdControlRepository {
    override suspend fun shouldShowBannerAd() = !appStorage.isFirstRun() &&
        appStorage.getPremiumEndDate().isPassed() &&
        appStorage.getSessionCount() > adConfigService.config.bannerAdSessionCount

    override suspend fun shouldShowInterstitialAd() = appStorage.getPremiumEndDate().isPassed() &&
        appStorage.getSessionCount() > adConfigService.config.interstitialAdSessionCount
}
