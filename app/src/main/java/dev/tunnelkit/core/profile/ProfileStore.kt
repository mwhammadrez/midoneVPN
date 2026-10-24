package dev.tunnelkit.core.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed profile storage.
 *
 * - Secrets (UUIDs inside links, WireGuard private keys) live only in encrypted preferences.
 * - One-way migration from the legacy plaintext store; the plaintext copy is removed afterwards.
 * - If the Keystore key is invalidated or the file is corrupted, the store throws instead of
 *   silently falling back to plaintext (a silent fallback would be a credential leak).
 */
class ProfileStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy { openEncrypted() }

    private fun openEncrypted(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encrypted = EncryptedSharedPreferences.create(
            appContext,
            ENCRYPTED_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        migrateLegacy(encrypted)
        return encrypted
    }

    /** Copies legacy plaintext profiles once, then deletes the plaintext copy. */
    private fun migrateLegacy(target: SharedPreferences) {
        if (target.getBoolean(KEY_MIGRATED, false)) return
        val legacy = appContext.getSharedPreferences(LEGACY_FILE, Context.MODE_PRIVATE)
        val raw = legacy.getString(LEGACY_KEY_LIST, null)
        val selected = legacy.getString(LEGACY_KEY_SELECTED, null)
        if (!raw.isNullOrBlank()) {
            target.edit()
                .putString(KEY_LIST, ProfileCodec.encode(ProfileCodec.decode(raw)))
                .putString(KEY_SELECTED, selected)
                .putBoolean(KEY_MIGRATED, true)
                .apply()
            legacy.edit().clear().apply()
        } else {
            target.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }

    fun list(): List<Profile> = ProfileCodec.decode(prefs.getString(KEY_LIST, null) ?: "")

    fun selectedId(): String? = prefs.getString(KEY_SELECTED, null)

    fun selected(): Profile? {
        val all = list()
        return all.firstOrNull { it.id == selectedId() } ?: all.firstOrNull()
    }

    fun add(profile: Profile): List<Profile> {
        val all = list().filterNot { it.id == profile.id } + profile
        prefs.edit().putString(KEY_LIST, ProfileCodec.encode(all)).apply()
        select(profile.id)
        return all
    }

    fun remove(id: String): List<Profile> {
        val all = list().filterNot { it.id == id }
        prefs.edit().putString(KEY_LIST, ProfileCodec.encode(all)).apply()
        if (selectedId() == id) prefs.edit().putString(KEY_SELECTED, all.firstOrNull()?.id).apply()
        return all
    }

    fun select(id: String) {
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val ENCRYPTED_FILE = "profiles_enc"
        const val LEGACY_FILE = "profiles"
        const val KEY_LIST = "profile_list_v1"
        const val KEY_SELECTED = "profile_selected"
        const val KEY_MIGRATED = "migrated_v1"
        const val LEGACY_KEY_LIST = "profile_list_v1"
        const val LEGACY_KEY_SELECTED = "profile_selected"
    }
}
