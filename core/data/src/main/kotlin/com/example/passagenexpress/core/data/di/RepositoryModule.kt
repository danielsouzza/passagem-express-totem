package com.example.passagenexpress.core.data.di

import com.example.passagenexpress.core.data.repository.ComodoRepositoryImpl
import com.example.passagenexpress.core.data.repository.PagamentoRepositoryImpl
import com.example.passagenexpress.core.data.repository.PedidoRepositoryImpl
import com.example.passagenexpress.core.data.repository.PointTapRepositoryImpl
import com.example.passagenexpress.core.data.repository.ViagemRepositoryImpl
import com.example.passagenexpress.core.domain.repository.ComodoRepository
import com.example.passagenexpress.core.domain.repository.PagamentoRepository
import com.example.passagenexpress.core.domain.repository.PedidoRepository
import com.example.passagenexpress.core.domain.repository.PointTapRepository
import com.example.passagenexpress.core.domain.repository.ViagemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindViagemRepository(impl: ViagemRepositoryImpl): ViagemRepository

    @Binds
    @Singleton
    abstract fun bindComodoRepository(impl: ComodoRepositoryImpl): ComodoRepository

    @Binds
    @Singleton
    abstract fun bindPedidoRepository(impl: PedidoRepositoryImpl): PedidoRepository

    @Binds
    @Singleton
    abstract fun bindPagamentoRepository(impl: PagamentoRepositoryImpl): PagamentoRepository

    @Binds
    @Singleton
    abstract fun bindPointTapRepository(impl: PointTapRepositoryImpl): PointTapRepository
}
