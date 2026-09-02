package com.example.passagenexpress.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.passagenexpress.core.domain.model.AppLanguage
import com.example.passagenexpress.core.domain.model.PaperWidth
import com.example.passagenexpress.core.domain.model.Porto
import com.example.passagenexpress.core.domain.model.TotemConfig
import com.example.passagenexpress.core.domain.repository.TotemConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TotemConfigDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : TotemConfigRepository {

    override val config: Flow<TotemConfig> = dataStore.data.map { prefs ->
        TotemConfig(
            subdomain = prefs[Keys.Subdomain].orEmpty(),
            portoId = prefs[Keys.PortoId] ?: 0L,
            portoSlug = prefs[Keys.PortoSlug].orEmpty(),
            portoNome = prefs[Keys.PortoNome].orEmpty(),
            municipioCodigo = prefs[Keys.MunicipioCodigo].orEmpty(),
            municipioNome = prefs[Keys.MunicipioNome].orEmpty(),
            defaultLanguage = prefs[Keys.Language]
                ?.let { tag -> AppLanguage.entries.firstOrNull { it.tag == tag } }
                ?: AppLanguage.PtBr,
            setupComplete = prefs[Keys.SetupComplete] ?: false,
            printerVendorId = prefs[Keys.PrinterVendorId],
            printerProductId = prefs[Keys.PrinterProductId],
            printerPaperWidth = prefs[Keys.PrinterPaperWidth]
                ?.let { name -> PaperWidth.entries.firstOrNull { it.name == name } }
                ?: PaperWidth.MM58,
            operatorPin = prefs[Keys.OperatorPin].orEmpty(),
            embarcacaoId = prefs[Keys.EmbarcacaoId],
            embarcacaoNome = prefs[Keys.EmbarcacaoNome].orEmpty(),
        )
    }

    override suspend fun setSubdomain(subdomain: String) {
        dataStore.edit { it[Keys.Subdomain] = subdomain }
    }

    override suspend fun setPorto(porto: Porto) {
        dataStore.edit {
            it[Keys.PortoId] = porto.id
            it[Keys.PortoSlug] = porto.slug
            it[Keys.PortoNome] = porto.nome
            it[Keys.MunicipioCodigo] = porto.municipioCodigo
            it[Keys.MunicipioNome] = porto.municipioNome
        }
    }

    override suspend fun clearPorto() {
        dataStore.edit {
            it.remove(Keys.PortoId)
            it.remove(Keys.PortoSlug)
            it.remove(Keys.PortoNome)
            it.remove(Keys.MunicipioCodigo)
            it.remove(Keys.MunicipioNome)
        }
    }

    override suspend fun setEmbarcacao(id: Long?, nome: String) {
        dataStore.edit {
            if (id == null) {
                it.remove(Keys.EmbarcacaoId)
                it.remove(Keys.EmbarcacaoNome)
            } else {
                it[Keys.EmbarcacaoId] = id
                it[Keys.EmbarcacaoNome] = nome
            }
        }
    }

    override suspend fun setPrinter(vendorId: Int, productId: Int) {
        dataStore.edit {
            it[Keys.PrinterVendorId] = vendorId
            it[Keys.PrinterProductId] = productId
        }
    }

    override suspend fun setPrinterPaperWidth(paperWidth: PaperWidth) {
        dataStore.edit { it[Keys.PrinterPaperWidth] = paperWidth.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.Language] = language.tag }
    }

    override suspend fun setOperatorPin(pin: String) {
        dataStore.edit { it[Keys.OperatorPin] = pin }
    }

    override suspend fun completeSetup() {
        dataStore.edit { it[Keys.SetupComplete] = true }
    }

    override suspend fun resetSetup() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val Subdomain = stringPreferencesKey("totem_subdomain")
        val PortoId = longPreferencesKey("totem_porto_id")
        val PortoSlug = stringPreferencesKey("totem_porto_slug")
        val PortoNome = stringPreferencesKey("totem_porto_nome")
        val MunicipioCodigo = stringPreferencesKey("totem_municipio_codigo")
        val MunicipioNome = stringPreferencesKey("totem_municipio_nome")
        val Language = stringPreferencesKey("totem_language")
        val SetupComplete = booleanPreferencesKey("totem_setup_complete")
        val PrinterVendorId = intPreferencesKey("totem_printer_vendor_id")
        val PrinterProductId = intPreferencesKey("totem_printer_product_id")
        val PrinterPaperWidth = stringPreferencesKey("totem_printer_paper_width")
        val OperatorPin = stringPreferencesKey("totem_operator_pin")
        val EmbarcacaoId = longPreferencesKey("totem_embarcacao_id")
        val EmbarcacaoNome = stringPreferencesKey("totem_embarcacao_nome")
    }
}
