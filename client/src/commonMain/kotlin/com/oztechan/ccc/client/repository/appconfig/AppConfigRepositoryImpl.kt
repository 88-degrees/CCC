package com.oztechan.ccc.client.repository.appconfig

import com.github.submob.scopemob.mapTo
import com.github.submob.scopemob.whether
import com.oztechan.ccc.client.BuildKonfig
import com.oztechan.ccc.client.model.Device
import com.oztechan.ccc.common.datasource.settings.SettingsDataSource
import com.oztechan.ccc.config.ConfigService

class AppConfigRepositoryImpl(
    private val configService: ConfigService,
    private val settingsDataSource: SettingsDataSource,
    private val device: Device
) : AppConfigRepository {
    override fun getDeviceType(): Device = device

    override fun getMarketLink(): String = device.marketLink

    override fun checkAppUpdate(
        isAppUpdateShown: Boolean
    ): Boolean? = configService.appConfig
        .appUpdate
        .firstOrNull { it.name == device.name }
        ?.whether(
            { !isAppUpdateShown },
            { updateLatestVersion > BuildKonfig.versionCode }
        )?.let {
            it.updateForceVersion <= BuildKonfig.versionCode
        }

    override fun shouldShowAppReview(): Boolean = configService.appConfig
        .appReview
        .whether { settingsDataSource.sessionCount > it.appReviewSessionCount }
        ?.mapTo { true }
        ?: false
}
