package com.apkpatcher.dex;

import com.apkpatcher.util.Log;

import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.TypeReference;

public class PatchMethod {

    private static final String BUNDLE = "Landroid/os/Bundle;";
    private static final String LIST = "Ljava/util/List;";
    private static final String STRING = "Ljava/lang/String;";
    private static final String CERTIFICATE = "Ljava/security/cert/Certificate;";
    private static final String FUNCTION0 = "Lkotlin/jvm/functions/Function0;";
    private static final String SSL_EXCEPTION = "Ljavax/net/ssl/SSLPeerUnverifiedException;";
    private static final String SSL_SESSION = "Ljavax/net/ssl/SSLSession;";
    private static final String SSL_SOCKET = "Ljavax/net/ssl/SSLSocket;";
    private static final String X509_CERT = "Ljava/security/cert/X509Certificate;";

    public static Method patch(Method method) {

        var name = method.getName();
        var params = method.getParameterTypes();
        var returnType = method.getReturnType();

        var implementation = method.getImplementation();

        /* ===== HostnameVerifier / OkHostnameVerifier / AbstractVerifier verify ===== */
        /*
        if (name.equals("verify")
                && params.size() == 2
                && params.contains(STRING)
                && (params.contains(SSL_SESSION)
                        || params.contains(X509_CERT)
                        || params.contains(SSL_SOCKET))
                && returnType.equals("Z")) {

            Log.o("[PATCH]", "HostnameVerifier ➢ verify(String, SSLSession|SSLSocket|X509Certificate)Z");

            return Patch.returnValue(method, true);
        }
        */

        /* ===== verify ➢ boolean =====
         * verify(String, SSLSession;)Z
         * verify(String, X509Certificate;)Z
         * verify(String, SSLSocket;)Z
        */
        // --- When method name is obfuscate
        if (implementation != null
                && returnType.equals("Z")
                && params.size() == 2
                && params.contains(STRING)
                && (params.contains(SSL_SESSION)
                        || params.contains(X509_CERT)
                        || params.contains(SSL_SOCKET))) {

            Log.o("[PATCH]", "HostnameVerifier ➢ verify (boolean)");

            return Patch.returnValue(method, true);
        }

        /* ===== verify ➢ void =====
         * verify(String, SSLSession;)V
         * verify(String, X509Certificate;)V
         * verify(String, SSLSocket;)V
        */
        // --- When method name is obfuscate
        if (implementation != null
                && returnType.equals("V")
                && params.size() == 2
                && params.contains(STRING)
                && (params.contains(SSL_SESSION)
                        || params.contains(X509_CERT)
                        || params.contains(SSL_SOCKET))) {

            Log.o("[PATCH]", "HostnameVerifier ➢ verify (void)");

            return Patch.returnVoid(method);
        }

        /* ===== AbstractVerifier verify(String, String[], String[], boolean) (void) ===== */
        if (name.equals("verify")
                && params.size() == 4
                && params.contains(STRING)
                && params.contains("[" + STRING)
                && params.contains("[" + STRING)
                && returnType.equals("V")) {

            Log.o("[PATCH]", "AbstractVerifier ➢ verify(String, String[], String[], Boolean)V");

            return Patch.returnVoid(method);
        }
        /* ===== AbstractVerifier verify(String, String[], String[]) ➢ void ===== */
        // --- IBM WorkLight
        if (name.equals("verify")
                && returnType.equals("V")
                && params.size() == 3
                && params.contains(STRING)
                && params.contains("[" + STRING)
                && params.contains("[" + STRING)) {

            Log.o("[PATCH]", "AbstractVerifier ➢ verify(String, String[], String[])V");

            return Patch.returnVoid(method);
        }

        /* ===== TrustManager checkClientTrusted / checkServerTrusted (void) ===== */
        if ((name.equals("checkClientTrusted") || name.equals("checkServerTrusted"))
                && returnType.equals("V")
                && params.stream().anyMatch(param -> param.toString().contains(X509_CERT))) {

            Log.o("[PATCH]", "TrustManager ➢ check(Client|Server)Trusted");

            return Patch.returnVoid(method);

        }

        /* ===== TrustManagerImpl checkServerTrusted (List) ===== */
        if (name.equals("checkServerTrusted")
                && returnType.equals(LIST)
                && params.stream().anyMatch(param -> param.toString().contains(X509_CERT))) {

            Log.o("[PATCH]", "TrustManagerImpl ➢ checkServerTrusted");

            return Patch.returnNull(method);
        }

        /* ===== CertificatePinner check ===== */
        if (name.equals("check")
                && returnType.equals("V")
                && params.contains(STRING)
                && (params.contains(LIST) || params.contains("[" + CERTIFICATE))) {

            Log.o("[PATCH]", "CertificatePinner ➢ check");

            return Patch.returnVoid(method);

        }

        /* ===== CertificatePinner check$okhttp ===== */
        if (name.equals("check$okhttp")
                && returnType.equals("V")
                && params.contains(STRING)) {

            Log.o("[PATCH]", "CertificatePinner ➢ check$okhttp");

            return Patch.returnVoid(method);

        }

        /* ===== CertificatePinner check$okhttp ===== */
        /*
        // --- When method name is obfuscate
        // --- Need improving detection ( may be now find worng method )
        if (implementation != null
                && returnType.equals("V")
                && params.contains(STRING)
                && params.contains(FUNCTION0)) {

            Log.o("[PATCH]", "CertificatePinner ➢ check$okhttp");

            return Patch.returnVoid(method);

        }
        */

        /* ===== X509TrustManager getAcceptedIssuers ===== */
        if (name.equals("getAcceptedIssuers")
                && returnType.equals("[" + X509_CERT)) {

            Log.o("[PATCH]", "X509TrustManager ➢ getAcceptedIssuers");

            return Patch.returnEmptyArray(method);
        }

        /* ===== OKHTTP3 SSL_EXCEPTION ===== */
        if (implementation != null
                && returnType.equals("V")
                && params.contains(STRING)) {

            boolean hasSSLException = false;
            boolean hasX509 = false;

            /*
            for (var annotation : method.getAnnotations()) {
                if (annotation.getType().equals("Ldalvik/annotation/Throws;")) {
                    for (var element : annotation.getElements()) {
                        if (element.getValue().toString().contains(SSL_EXCEPTION)) {
                            hasSSLException = true;
                        }
                    }
                }
            }
            */

            for (var instruction : implementation.getInstructions()) {
                if (instruction instanceof ReferenceInstruction refInstruction) {
                    var ref = refInstruction.getReference();
                    if (ref instanceof TypeReference typeRef) {
                        var type = typeRef.getType();
                        if (instruction.getOpcode() == Opcode.CHECK_CAST && X509_CERT.equals(type)) {
                            hasX509 = true;
                        }
                        if (hasX509 && SSL_EXCEPTION.equals(type)) {
                            hasSSLException = true;
                        }
                    }
                }
            }
            if (hasSSLException) {
                Log.o("[PATCH]", "OKHTTP3 ➢ SSL_EXCEPTION ");
                return Patch.returnVoid(method);
            }
        }

        /* ===== License ===== */
        if ((name.equals("connectToLicensingService")
                || name.equals("initializeLicenseCheck"))
                && returnType.equals("V")) {

            Log.o("[PATCH]", "LicenseClient ➢ connectToLicensingService & initializeLicenseCheck");

            return Patch.returnVoid(method);
        }

        if (name.equals("processResponse")
                && returnType.equals("V")
                && params.contains("I")
                && params.contains(BUNDLE)) {

            Log.o("[PATCH]", "LicenseClient ➢ processResponse");

            return Patch.returnVoid(method);
        }

        return method;
    }
}