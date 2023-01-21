package com.oztechan.ccc.common.service

import com.oztechan.ccc.common.core.network.api.backend.BackendApi
import com.oztechan.ccc.common.core.network.error.UnknownNetworkException
import com.oztechan.ccc.common.core.network.mapper.toExchangeRateModel
import com.oztechan.ccc.common.core.network.model.Conversion
import com.oztechan.ccc.common.core.network.model.ExchangeRate
import com.oztechan.ccc.common.service.backend.BackendApiService
import com.oztechan.ccc.common.service.backend.BackendApiServiceImpl
import com.oztechan.ccc.test.BaseSubjectTest
import com.oztechan.ccc.test.util.createTestDispatcher
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("OPT_IN_USAGE")
internal class BackendApiServiceTest : BaseSubjectTest<BackendApiService>() {

    override val subject: BackendApiService by lazy {
        BackendApiServiceImpl(backendApi, createTestDispatcher())
    }

    @Mock
    private val backendApi = mock(classOf<BackendApi>())

    private val exchangeRate = ExchangeRate("EUR", "12.21.2121", Conversion())
    private val throwable = Throwable("mock")
    private val base = "EUR"

    @Test
    fun `getConversion parameter can not be empty`() = runTest {
        runCatching { subject.getConversion("") }.let {
            assertFalse { it.isSuccess }
            assertTrue { it.isFailure }
            assertIs<UnknownNetworkException>(it.exceptionOrNull())
        }

        verify(backendApi)
            .coroutine { backendApi.getConversion("") }
            .wasInvoked()
    }

    @Test
    fun `getConversion error`() = runTest {
        given(backendApi)
            .coroutine { backendApi.getConversion(base) }
            .thenThrow(throwable)

        runCatching { subject.getConversion(base) }.let {
            assertFalse { it.isSuccess }
            assertTrue { it.isFailure }
            assertNotNull(it.exceptionOrNull())
            assertNotNull(it.exceptionOrNull()!!.cause)
            assertNotNull(it.exceptionOrNull()!!.message)
            assertEquals(throwable.message, it.exceptionOrNull()!!.cause!!.message)
            assertIs<UnknownNetworkException>(it.exceptionOrNull())
        }

        verify(backendApi)
            .coroutine { getConversion(base) }
            .wasInvoked()
    }

    @Test
    fun `getConversion success`() = runTest {
        given(backendApi)
            .coroutine { backendApi.getConversion(base) }
            .thenReturn(exchangeRate)

        runCatching { subject.getConversion(base) }.let {
            assertTrue { it.isSuccess }
            assertFalse { it.isFailure }
            assertEquals(exchangeRate.toExchangeRateModel(), it.getOrNull())
        }

        verify(backendApi)
            .coroutine { getConversion(base) }
            .wasInvoked()
    }
}
