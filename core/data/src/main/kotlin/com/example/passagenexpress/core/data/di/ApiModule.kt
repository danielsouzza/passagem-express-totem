package com.example.passagenexpress.core.data.di

import com.example.passagenexpress.core.data.remote.api.ComodoApi
import com.example.passagenexpress.core.data.remote.api.PagamentoApi
import com.example.passagenexpress.core.data.remote.api.PedidoApi
import com.example.passagenexpress.core.data.remote.api.PointTapApi
import com.example.passagenexpress.core.data.remote.api.ViagemApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideViagemApi(retrofit: Retrofit): ViagemApi = retrofit.create(ViagemApi::class.java)

    @Provides
    @Singleton
    fun provideComodoApi(retrofit: Retrofit): ComodoApi = retrofit.create(ComodoApi::class.java)

    @Provides
    @Singleton
    fun providePedidoApi(retrofit: Retrofit): PedidoApi = retrofit.create(PedidoApi::class.java)

    @Provides
    @Singleton
    fun providePagamentoApi(retrofit: Retrofit): PagamentoApi = retrofit.create(PagamentoApi::class.java)

    @Provides
    @Singleton
    fun providePointTapApi(retrofit: Retrofit): PointTapApi = retrofit.create(PointTapApi::class.java)
}
