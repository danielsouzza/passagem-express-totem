package com.example.passagenexpress.core.printer.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import com.example.passagenexpress.core.common.result.AppError
import com.example.passagenexpress.core.common.result.AppResult
import com.example.passagenexpress.core.domain.model.BilheteMapeado
import com.example.passagenexpress.core.domain.printer.TicketPrinter
import com.example.passagenexpress.core.domain.printer.UsbPrinterDevice
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import com.example.passagenexpress.core.printer.escpos.EscPosTicketBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

    override fun listDevices(): List<UsbPrinterDevice> =
        usbManager.deviceList.values.map { device ->
            UsbPrinterDevice(
                vendorId = device.vendorId,
                productId = device.productId,
                name = device.productName ?: device.deviceName,
            )
        }

    override suspend fun print(bilhete: BilheteMapeado): AppResult<Unit> =
        send(EscPosTicketBuilder.build(bilhete))

    override suspend fun testPrint(): AppResult<Unit> =
        send(EscPosTicketBuilder.buildTest())

    private suspend fun send(payload: ByteArray): AppResult<Unit> {
        val device = resolveDevice()
            ?: return AppResult.Failure(AppError.NotFound(message = "Impressora USB não encontrada"))

        if (!usbManager.hasPermission(device)) {
            val granted = requestPermission(device)
            if (!granted) {
                return AppResult.Failure(
                    AppError.Unauthorized(message = "Permissão de acesso à impressora negada"),
                )
            }
        }
        return withContext(Dispatchers.IO) { writeToDevice(device, payload) }
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

    private fun writeToDevice(device: UsbDevice, payload: ByteArray): AppResult<Unit> {
        val iface = device.printerInterface()
            ?: return AppResult.Failure(AppError.Unknown(message = "Interface de impressão não encontrada"))
        val endpoint = iface.bulkOutEndpoint()
            ?: return AppResult.Failure(AppError.Unknown(message = "Endpoint de saída não encontrado"))

        var connection: UsbDeviceConnection? = null
        return try {
            connection = usbManager.openDevice(device)
                ?: return AppResult.Failure(AppError.Unknown(message = "Falha ao abrir a impressora"))
            if (!connection.claimInterface(iface, true)) {
                return AppResult.Failure(AppError.Unknown(message = "Falha ao reservar a interface USB"))
            }
            var offset = 0
            while (offset < payload.size) {
                val chunk = minOf(CHUNK_SIZE, payload.size - offset)
                val sent = connection.bulkTransfer(endpoint, payload, offset, chunk, TRANSFER_TIMEOUT_MS)
                if (sent < 0) {
                    return AppResult.Failure(AppError.Unknown(message = "Falha ao enviar dados para a impressora"))
                }
                offset += if (sent == 0) chunk else sent
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(message = e.message, cause = e))
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
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
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
        const val ACTION_USB_PERMISSION = "com.example.passagenexpress.USB_PERMISSION"
        const val CHUNK_SIZE = 16 * 1024
        const val TRANSFER_TIMEOUT_MS = 5_000
    }
}
