package dev.tunnelkit.engine.xray

import libXray.DialerController
import libXray.LibXray

/**
 * Real binding to libXray v26.10.21 (MIT), artifact `app/libs/libXRay.aar`.
 *
 * Loading this class pulls in the Go runtime (`Seq.touch()`), so it must never be constructed in a
 * unit test — tests use [XrayCore] fakes instead.
 */
class LibXrayCore : XrayCore {

    override val apiVersion: Long = LibXray.LibXrayAPIVersion

    private var protector: ((Long) -> Boolean)? = null

    private val controller = object : DialerController {
        override fun protectFd(fd: Long): Boolean = protector?.invoke(fd) ?: false
    }

    override fun invoke(requestJson: String): String = LibXray.invoke(requestJson)

    override fun setDns(server: String) {
        LibXray.registerDialerController(controller)
        LibXray.setDNS(controller, server)
    }

    override fun resetDns() {
        runCatching { LibXray.resetDNS() }
    }

    override fun registerProtector(protect: (Long) -> Boolean) {
        protector = protect
        LibXray.registerDialerController(controller)
    }
}
