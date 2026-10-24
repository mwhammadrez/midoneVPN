package dev.tunnelkit.core.profile

/** Pure encode/decode so the storage format is testable without Android. */
object ProfileCodec {

    fun encode(items: List<Profile>): String = items.joinToString("\n") { p ->
        listOf(p.id, p.name.replace('\t', ' '), p.link, p.transport, p.security).joinToString("\t")
    }

    fun decode(raw: String): List<Profile> = raw.lineSequence()
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
}
