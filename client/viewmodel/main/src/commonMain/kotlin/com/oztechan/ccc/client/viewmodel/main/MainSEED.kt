package com.oztechan.ccc.client.viewmodel.main

import com.oztechan.ccc.client.core.viewmodel.BaseData
import com.oztechan.ccc.client.core.viewmodel.BaseEffect
import com.oztechan.ccc.client.core.viewmodel.BaseEvent
import com.oztechan.ccc.client.core.viewmodel.BaseState
import kotlinx.coroutines.Job

// State
data class MainState(
    var shouldOnboardUser: Boolean? = null,
    var appTheme: Int? = null
) : BaseState

// Effect
sealed class MainEffect : BaseEffect {
    data object ShowInterstitialAd : MainEffect()
    data object RequestReview : MainEffect()
    data class AppUpdateEffect(val isCancelable: Boolean, val marketLink: String) : MainEffect()
}

// Event
interface MainEvent : BaseEvent {
    fun onPause()
    fun onResume()
}

// Data
data class MainData(
    var adJob: Job = Job(),
    var adVisibility: Boolean = false,
    var isAppUpdateShown: Boolean = false,
    var isNewSession: Boolean = true
) : BaseData
