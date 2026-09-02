package com.example.passagenexpress

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * Rede de segurança de última instância para o totem.
 *
 * Um totem de autoatendimento não pode, em hipótese alguma, "cair para o launcher": ficaria com a
 * área de trabalho do Android exposta e inutilizável até alguém ir presencialmente reiniciar. As
 * camadas de dados já convertem respostas fora do padrão em erros tratáveis, mas nenhuma cobertura
 * é 100% — uma exceção imprevista na UI/Compose ou numa thread de hardware ainda mataria o processo.
 *
 * Este handler intercepta QUALQUER exceção não tratada em QUALQUER thread, registra o erro e reabre
 * o app automaticamente via [AlarmManager]. Um backoff simples evita loop de crash apertado quando
 * a falha acontece logo na abertura.
 */
object TotemCrashRecovery {

    private const val TAG = "TotemCrash"
    private const val PREFS = "totem_crash_recovery"
    private const val KEY_LAST_CRASH_AT = "last_crash_at"
    private const val KEY_CRASH_COUNT = "crash_count"

    /** Crashes dentro desta janela contam como "loop" e aumentam o atraso de reinício. */
    private const val RAPID_WINDOW_MS = 10_000L

    /** Atraso base do reinício; cresce por crash rápido até o teto. */
    private const val BASE_RESTART_DELAY_MS = 800L
    private const val MAX_BACKOFF_STEPS = 6

    /** Após este tempo aberto sem crash, considera-se estável e o backoff é zerado. */
    private const val STABLE_AFTER_MS = 20_000L

    fun install(app: Application) {
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            handle(app, thread, error)
        }
    }

    private fun handle(app: Application, thread: Thread, error: Throwable) {
        Log.e(TAG, "Exceção não tratada em '${thread.name}' — reiniciando o totem", error)
        val delayMs = try {
            registerCrashAndComputeDelay(app)
        } catch (e: Throwable) {
            Log.e(TAG, "Falha ao calcular backoff de reinício", e)
            BASE_RESTART_DELAY_MS
        }
        try {
            scheduleRestart(app, delayMs)
        } catch (e: Throwable) {
            Log.e(TAG, "Falha ao agendar reinício do totem", e)
        }
        // O processo pode estar em estado inconsistente; encerra e deixa o AlarmManager reabri-lo.
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    /**
     * Deve ser chamado pela Activity depois de a tela sobreviver [STABLE_AFTER_MS] sem crash.
     * Zera o contador de backoff, de modo que uma falha futura reinicie rápido de novo.
     */
    fun markLaunchStable(context: Context) {
        runCatching {
            prefs(context).edit().putInt(KEY_CRASH_COUNT, 0).apply()
        }
    }

    private fun registerCrashAndComputeDelay(context: Context): Long {
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        val lastCrash = prefs.getLong(KEY_LAST_CRASH_AT, 0L)
        val previousCount = prefs.getInt(KEY_CRASH_COUNT, 0)
        val count = if (now - lastCrash in 0..RAPID_WINDOW_MS) previousCount + 1 else 1
        prefs.edit()
            .putLong(KEY_LAST_CRASH_AT, now)
            .putInt(KEY_CRASH_COUNT, count)
            .apply()
        val step = count.coerceAtMost(MAX_BACKOFF_STEPS)
        return BASE_RESTART_DELAY_MS * step
    }

    private fun scheduleRestart(context: Context, delayMs: Long) {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
            ?: return
        val pending = PendingIntent.getActivity(
            context,
            RESTART_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + delayMs, pending)
    }

    private const val RESTART_REQUEST_CODE = 424242

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Exposto para a Activity agendar a marcação de estabilidade sem conhecer as constantes. */
    val stableAfterMs: Long get() = STABLE_AFTER_MS
}
