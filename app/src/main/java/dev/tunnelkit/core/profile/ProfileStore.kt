package dev.tunnelkit.core.profile

import android.content.Context

/**
 * Persists imported profiles. Plain prefs for now (tracked limitation);
 * the contract stays the same when this moves to a Keystore-backed store.
 */
class ProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    fun list(): List<Profile> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return decode(raw)
    }

    fun selectedId(): String? = prefs.getString(KEY_SELECTED, null)

    fun selected(): Profile? {
        val all = list()
        return all.firstOrNull { it.id == selectedId() } ?: all.firstOrNull()
    }

    fun add(profile: Profile): List<Profile> {
        val all = list().filterNot { it.id == profile.id } + profile
        prefs.edit().putString(KEY_LIST, encode(all)).apply()
        select(profile.id)
        return all
    }

    fun remove(id: String): List<Profile> {
        val all = list().filterNot { it.id == id }
        prefs.edit().putString(KEY_LIST, encode(all)).apply()
        if (selectedId() == id) prefs.edit().putString(KEY_SELECTED, all.firstOrNull()?.id).apply()
        return all
    }

    fun select(id: String) {
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    /** Tab-separated records; links are single-line by nature. */
    private fun encode(items: List<Profile>): String = items.joinToString("\n") { p ->
        listOf(p.id, p.name.replace('\t', ' '), p.link, p.transport, p.security).joinToString("\t")
    }

    private fun decode(raw: String): List<Profile> = raw.lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val f = line.split('\t')
            if (f.size < 3) null else Profile(
                id = f[0],
                name = f[1],
                link = f[2],
                transport = f.getOrElse(3) { "" },
                security = f.getOrElse(4) { "" },
            )
        }
        .toList()

    private companion object {
        const val KEY_LIST = "profile_list_v1"
        const val KEY_SELECTED = "profile_selected"
    }
}
