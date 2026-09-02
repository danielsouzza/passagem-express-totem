package com.example.passagenexpress.core.printer.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.example.passagenexpress.core.common.result.AppError
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.BilheteMapeado
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.printer.TicketPrinter
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import com.example.passagenexpress.core.printer.escpos.EscPosTicketBuilder
import com.example.passagenexpress.core.printer.escpos.Pdf417Raster
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementação USB nativa ([UsbManager]) da [TicketPrinter] — sem SDK de terceiros.
 * Resolve o device pelo VID/PID salvo no setup, garante permissão runtime, abre a interface
 * Printer (classe 7) e envia os bytes ESC/POS por bulk transfer.
 */
@Singleton
internal class UsbTicketPrinter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val totemConfigRepository: TotemConfigRepository,
) : TicketPrinter {

    private val usbManager: UsbManager
        get() = context.getSystemService(Context.USB_SERVICE) as UsbManager

    /** Serializa todo acesso ao device: jobs reais de impressão e pings de keep-alive nunca
     *  abrem/escrevem na interface USB ao mesmo tempo. */
    private val deviceMutex = Mutex()

    override fun listDevices(): List<UsbPrinterDevice> =
        usbManager.deviceList.values.map { device ->
            UsbPrinterDevice(
                vendorId = device.vendorId,
                productId = device.productId,
                name = device.productName ?: device.deviceName,
            )
        }

    override suspend fun print(bilhete: BilheteMapeado): AppResult<Unit> {
        // Debug sem impressora conectada: gera o ESC/POS em 58mm e salva num .bin pra envio
        // manual (permite testar o fluxo sem hardware). Em release, segue o caminho normal.
        if (isDebuggable && resolveDevice() == null) {
            val logo = fetchLogoRaster(bilhete.logo, logoWidthFor(PaperWidth.MM58))
            val bytes = EscPosTicketBuilder.build(bilhete, logoRaster = logo, paper = PaperWidth.MM58)
            return dumpToFile(bytes, "bilhete")
        }
        val paper = totemConfigRepository.config.first().printerPaperWidth
        val logo = fetchLogoRaster(bilhete.logo, logoWidthFor(paper))
        return send(EscPosTicketBuilder.build(bilhete, logoRaster = logo, paper = paper))
    }

    override suspend fun testPrint(): AppResult<Unit> {
        if (isDebuggable && resolveDevice() == null) {
            return dumpToFile(EscPosTicketBuilder.buildTest(PaperWidth.MM58), "teste")
        }
        val paper = totemConfigRepository.config.first().printerPaperWidth
        return send(EscPosTicketBuilder.buildTest(paper))
    }

    /** True em builds de debug (app debuggable) — habilita o fallback de salvar em arquivo. */
    private val isDebuggable: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /** Sequência p/ nomear os .bin sem colisão entre bilhetes do mesmo pedido. */
    private val dumpSeq = AtomicInteger(0)

    /**
     * Fallback de debug: grava os bytes ESC/POS em
     * `getExternalFilesDir/tickets/<label>-<millis>-<seq>.bin` (puxável via `adb pull`) e retorna
     * Success — o fluxo de impressão segue como se tivesse impresso.
     */
    private suspend fun dumpToFile(bytes: ByteArray, label: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.getExternalFilesDir(null), "tickets").apply { mkdirs() }
                val file = File(dir, "$label-${System.currentTimeMillis()}-${dumpSeq.incrementAndGet()}.bin")
                file.writeBytes(bytes)
                Log.i(TAG, "Sem impressora: ESC/POS salvo em ${file.absolutePath} (${bytes.size} bytes)")
                AppResult.Success(Unit)
            }.getOrElse { e ->
                AppResult.Failure(AppError.Unknown(message = e.message, cause = e))
            }
        }

    /** Logo a ~62% da largura da bobina (240 dots no 58mm, 360 no 80mm), centralizado. */
    private fun logoWidthFor(paper: PaperWidth): Int = paper.dots * 5 / 8

    /** URL do último logo baixado -> raster ESC/POS (ou null se falhou). Evita rebaixar a cada
     *  bilhete do mesmo pedido. */
    @Volatile
    private var cachedLogo: Pair<String, ByteArray?>? = null

    /**
     * Baixa a logomarca da URL e rasteriza pra ESC/POS. Best-effort: qualquer falha (rede,
     * decode) retorna null e o bilhete é impresso sem logo. Cacheia por URL.
     */
    private suspend fun fetchLogoRaster(url: String?, maxWidth: Int): ByteArray? {
        if (url.isNullOrBlank()) return null
        cachedLogo?.let { if (it.first == url) return it.second }
        return withContext(Dispatchers.IO) {
            val bytes = runCatching {
                (java.net.URL(url).openConnection() as java.net.HttpURLConnection).run {
                    connectTimeout = LOGO_FETCH_TIMEOUT_MS
                    readTimeout = LOGO_FETCH_TIMEOUT_MS
                    try {
                        if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                            inputStream.use { it.readBytes() }
                        } else {
                            null
                        }
                    } finally {
                        disconnect()
                    }
                }
            }.getOrNull()
            val raster = Pdf417Raster.toRasterFromBytes(bytes, maxWidth, upscale = true)
            cachedLogo = url to raster
            raster
        }
    }

    private suspend fun send(payload: ByteArray): AppResult<Unit> {
        val device = resolveDevice()
            ?: return AppResult.Failure(AppError.NotFound(message = "Impressora USB não encontrada"))

        if (!ensurePermission(device)) {
            return AppResult.Failure(
                AppError.Unauthorized(message = "Permissão de acesso à impressora negada"),
            )
        }

        // A abertura/claim/bulk-transfer USB falha esporadicamente (ex.: logo depois de o
        // bilhete anterior fechar a conexão, ou sob buffering da impressora). Em vez de exigir
        // que o operador toque "Tentar novamente", reexecutamos a escrita algumas vezes com um
        // breve intervalo de acomodação do barramento — exatamente o que o retry manual fazia.
        // Mas só reenviamos enquanto NADA foi impresso ainda: se a falha ocorreu no meio do
        // envio (papel já saiu), retransmitir o payload inteiro duplicaria/truncaria o bilhete,
        // então essa falha é surfada pra retry manual.
        return deviceMutex.withLock {
            var lastError: AppError? = null
            repeat(MAX_WRITE_ATTEMPTS) { attempt ->
                if (attempt > 0) delay(RETRY_DELAY_MS)
                val outcome = withContext(Dispatchers.IO) { writeToDevice(device, payload) }
                when (val result = outcome.result) {
                    is AppResult.Success -> return@withLock result
                    is AppResult.Failure -> {
                        lastError = result.error
                        if (!outcome.retriable) return@withLock result
                    }
                }
            }
            AppResult.Failure(lastError ?: AppError.Unknown(message = "Falha ao imprimir o bilhete"))
        }
    }

    /**
     * Ping de keep-alive: envia a consulta de status em tempo real `DLE EOT 1` (não move papel)
     * pra manter a impressora acordada quando o totem fica ocioso. Best-effort e silencioso:
     * se não há device, não há permissão ainda, ou o envio falha, simplesmente não faz nada
     * (nunca abre diálogo de permissão nem surfa erro). Compartilha o [deviceMutex] com [send],
     * então nunca colide com uma impressão real.
     */
    override suspend fun keepAlive() {
        val device = resolveDevice() ?: return
        if (!usbManager.hasPermission(device)) return
        deviceMutex.withLock {
            withContext(Dispatchers.IO) { runCatching { writeToDevice(device, KEEP_ALIVE_PAYLOAD) } }
        }
    }

    /**
     * Garante permissão de acesso ao device. O extra [UsbManager.EXTRA_PERMISSION_GRANTED] do
     * broadcast é **não-confiável** em vários OEMs (volta `false` mesmo após o usuário conceder),
     * então confirmamos sempre via [UsbManager.hasPermission] — que é a fonte da verdade e a
     * razão de "tocar de novo" funcionar.
     */
    private suspend fun ensurePermission(device: UsbDevice): Boolean {
        if (usbManager.hasPermission(device)) return true
        val granted = requestPermission(device)
        return granted || usbManager.hasPermission(device)
    }

    /** Device configurado (VID/PID); senão a 1ª interface classe Printer; senão o 1º device. */
    private suspend fun resolveDevice(): UsbDevice? {
        val devices = usbManager.deviceList.values
        if (devices.isEmpty()) return null
        val config = totemConfigRepository.config.first()
        val vid = config.printerVendorId
        val pid = config.printerProductId
        if (vid != null && pid != null) {
            devices.firstOrNull { it.vendorId == vid && it.productId == pid }?.let { return it }
        }
        return devices.firstOrNull { it.hasPrinterInterface() } ?: devices.firstOrNull()
    }

    /**
     * Resultado de uma tentativa de escrita. [retriable] indica que é seguro reexecutar o envio
     * inteiro — verdadeiro enquanto nenhum byte foi transferido (falha de open/claim/endpoint ou
     * timeout antes do 1º byte). Quando o papel já começou a sair, [retriable] = false.
     */
    private class WriteOutcome(val result: AppResult<Unit>, val retriable: Boolean)

    private fun writeToDevice(device: UsbDevice, payload: ByteArray): WriteOutcome {
        val iface = device.printerInterface()
            ?: return WriteOutcome(
                AppResult.Failure(AppError.Unknown(message = "Interface de impressão não encontrada")),
                retriable = true,
            )
        val endpoint = iface.bulkOutEndpoint()
            ?: return WriteOutcome(
                AppResult.Failure(AppError.Unknown(message = "Endpoint de saída não encontrado")),
                retriable = true,
            )

        var connection: UsbDeviceConnection? = null
        var offset = 0
        return try {
            connection = usbManager.openDevice(device)
                ?: return WriteOutcome(
                    AppResult.Failure(AppError.Unknown(message = "Falha ao abrir a impressora")),
                    retriable = true,
                )
            if (!connection.claimInterface(iface, true)) {
                return WriteOutcome(
                    AppResult.Failure(AppError.Unknown(message = "Falha ao reservar a interface USB")),
                    retriable = true,
                )
            }
            while (offset < payload.size) {
                val chunk = minOf(CHUNK_SIZE, payload.size - offset)
                val sent = connection.bulkTransfer(endpoint, payload, offset, chunk, TRANSFER_TIMEOUT_MS)
                if (sent < 0) {
                    return WriteOutcome(
                        AppResult.Failure(AppError.Unknown(message = "Falha ao enviar dados para a impressora")),
                        retriable = offset == 0,
                    )
                }
                offset += if (sent == 0) chunk else sent
            }
            WriteOutcome(AppResult.Success(Unit), retriable = false)
        } catch (e: Exception) {
            WriteOutcome(
                AppResult.Failure(AppError.Unknown(message = e.message, cause = e)),
                retriable = offset == 0,
            )
        } finally {
            connection?.releaseInterface(iface)
            connection?.close()
        }
    }

    private suspend fun requestPermission(device: UsbDevice): Boolean =
        suspendCancellableCoroutine { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != ACTION_USB_PERMISSION) return
                    context.unregisterReceiver(this)
                    // Confia no estado real do device, não só no extra do broadcast (que alguns
                    // OEMs entregam como `false` mesmo após o usuário conceder a permissão).
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) ||
                        usbManager.hasPermission(device)
                    if (cont.isActive) cont.resume(granted)
                }
            }
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
            cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val pending = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION).setPackage(context.packageName), flags,
            )
            usbManager.requestPermission(device, pending)
        }

    private fun UsbDevice.hasPrinterInterface(): Boolean = printerInterface() != null

    /** Interface de classe Printer (7); fallback pra primeira interface do device. */
    private fun UsbDevice.printerInterface(): UsbInterface? {
        for (i in 0 until interfaceCount) {
            val iface = getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return iface
        }
        return if (interfaceCount > 0) getInterface(0) else null
    }

    private fun UsbInterface.bulkOutEndpoint(): UsbEndpoint? {
        for (i in 0 until endpointCount) {
            val ep = getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                ep.direction == UsbConstants.USB_DIR_OUT
            ) {
                return ep
            }
        }
        return null
    }

    private companion object {
        const val TAG = "UsbTicketPrinter"
        const val ACTION_USB_PERMISSION = "com.example.passagenexpress.USB_PERMISSION"
        const val CHUNK_SIZE = 16 * 1024
        const val TRANSFER_TIMEOUT_MS = 8_000
        const val MAX_WRITE_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 350L

        /** `DLE EOT 1` — consulta de status em tempo real. A impressora responde 1 byte e
         *  **não imprime nada**; usado só como keep-alive pra evitar o sleep. */
        val KEEP_ALIVE_PAYLOAD = byteArrayOf(0x10, 0x04, 0x01)

        const val LOGO_FETCH_TIMEOUT_MS = 4_000
    }
}
