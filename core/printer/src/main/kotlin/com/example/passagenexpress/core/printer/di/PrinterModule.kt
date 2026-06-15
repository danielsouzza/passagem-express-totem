package com.example.passagenexpress.core.printer.di

import com.example.passagenexpress.core.domain.printer.TicketPrinter
import com.example.passagenexpress.core.printer.usb.UsbTicketPrinter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PrinterModule {

    @Binds
    @Singleton
    abstract fun bindTicketPrinter(impl: UsbTicketPrinter): TicketPrinter
}
