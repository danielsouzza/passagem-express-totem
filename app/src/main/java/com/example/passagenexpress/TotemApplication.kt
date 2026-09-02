package com.example.passagenexpress

import android.app.Application
import com.example.passagenexpress.core.printer.usb.PrinterKeepAlive
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TotemApplication : Application() {

    @Inject
    lateinit var printerKeepAlive: PrinterKeepAlive

    override fun onCreate() {
        super.onCreate()
        // Rede de segurança de última instância: qualquer exceção não tratada reinicia o totem em
        // vez de deixá-lo cair para o launcher. Instalado o mais cedo possível.
        TotemCrashRecovery.install(this)
        // Mantém a impressora térmica acordada enquanto o totem está ligado (evita o sleep que
        // fazia a 1ª impressão após ociosidade falhar).
        printerKeepAlive.start()
    }
}
