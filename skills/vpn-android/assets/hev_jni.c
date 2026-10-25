/* JNI bridge for hev-socks5-tunnel.
 * Drop into app/src/main/jni/ and let ndk-build compile it together with the library.
 * Symbols must match the Kotlin object: dev.tunnelkit.core.HevBridge
 */
#include <jni.h>
#include "hev-socks5-tunnel.h"

JNIEXPORT jint JNICALL
Java_dev_tunnelkit_core_HevBridge_init(JNIEnv *env, jobject thiz, jint tun_fd) {
    (void) env; (void) thiz;
    return hev_socks5_tunnel_init((int) tun_fd);
}

JNIEXPORT jint JNICALL
Java_dev_tunnelkit_core_HevBridge_run(JNIEnv *env, jobject thiz) {
    (void) env; (void) thiz;
    return hev_socks5_tunnel_run();
}

JNIEXPORT void JNICALL
Java_dev_tunnelkit_core_HevBridge_stop(JNIEnv *env, jobject thiz) {
    (void) env; (void) thiz;
    hev_socks5_tunnel_stop();
}

JNIEXPORT void JNICALL
Java_dev_tunnelkit_core_HevBridge_fini(JNIEnv *env, jobject thiz) {
    (void) env; (void) thiz;
    hev_socks5_tunnel_fini();
}

JNIEXPORT jlongArray JNICALL
Java_dev_tunnelkit_core_HevBridge_stats(JNIEnv *env, jobject thiz) {
    (void) thiz;
    size_t tx_packets = 0, tx_bytes = 0, rx_packets = 0, rx_bytes = 0;
    hev_socks5_tunnel_stats(&tx_packets, &tx_bytes, &rx_packets, &rx_bytes);
    jlong values[4] = {(jlong) tx_packets, (jlong) tx_bytes,
                       (jlong) rx_packets, (jlong) rx_bytes};
    jlongArray out = env->NewLongArray(4);
    if (out) env->SetLongArrayRegion(out, 0, 4, values);
    return out;
}
