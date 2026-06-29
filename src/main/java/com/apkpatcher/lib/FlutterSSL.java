package com.apkpatcher.lib;

import com.apkpatcher.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Patches libflutter.so to disable SSL certificate verification.
 *
 * Flutter uses BoringSSL (native C/C++) for TLS, completely bypassing Java's
 * TrustManager/SSLContext. This patcher finds the `ssl_verify_peer_cert` function
 * in the binary using byte patterns and patches it.
 *
 * Normal ssl_verify_peer_cert => return 0
 * ssl_crypto_x509_session_verify_cert_chain => return 1
 *
 * Byte patterns are sourced from:
 * https://github.com/NVISOsecurity/disable-flutter-tls-verification
 */
public class FlutterSSL {

    /* ===== Return-zero patch bytes per architecture ===== */

    private static final byte[] RETURN_ZERO_ARM64 = {
        0x00, 0x00, (byte) 0x80, 0x52,  // MOV W0, #0
        (byte) 0xC0, 0x03, 0x5F, (byte) 0xD6  // RET
    };

    private static final byte[] RETURN_ZERO_ARM32 = {
        0x00, 0x20,  // MOVS R0, #0
        0x70, 0x47   // BX LR
    };

    private static final byte[] RETURN_ZERO_X86_X64 = {
        0x31, (byte) 0xC0,  // XOR EAX, EAX
        (byte) 0xC3         // RET
    };

    /* ===== Return-one patch bytes per architecture ===== */

    private static final byte[] RETURN_ONE_ARM64 = {
        0x20, 0x00, (byte) 0x80, 0x52,  // MOV W0, #1
        (byte) 0xC0, 0x03, 0x5F, (byte) 0xD6  // RET
    };

    private static final byte[] RETURN_ONE_X86_X64 = {
        (byte) 0xB8, 0x01, 0x00, 0x00, 0x00,  // MOV EAX, 1
        (byte) 0xC3  // RET
    };

    /** ========== PATTERNS ==========
     * Patterns per architecture (from Frida script - NVISOsecurity)
     * Uses hex strings with ?? for wildcard nibbles
     */
    private static final Map<String, List<String>> PATTERNS = Map.of(
        "arm64", List.of(
            "F? 0F 1C F8 F? 5? 01 A9 F? 5? 02 A9 F? ?? 03 A9 ?? ?? ?? ?? 68 1A 40 F9",
            "F? 0F 1C F8 F? ?? 0? ?? ?? ?? ?? ?9 ?? ?? 0? ?? 68 1A 40 F9 15 ?? 4? F9 B5 00 00 B4 B6 46 40 F9",
            "F? 43 01 D1 FE 67 01 A9 F8 5F 02 A9 F6 57 03 A9 F4 4F 04 A9 13 00 40 F9 F4 03 00 AA 68 1A 40 F9",
            "FF 43 01 D1 FE 67 01 A9 ?? ?? 06 94 ?? 7? 06 94 68 1A 40 F9 15 15 41 F9 B5 00 00 B4 B6 4A 40 F9",
            "FF ?3 01 D1 F? ?? 01 A9 ?? ?? ?? 94 ?? ?? ?? 52 48 00 00 39 1A 50 40 F9 DA 02 00 B4 48 03 40 F9" // ssl_crypto_x509_session_verify_cert_chain
        ),

        "arm", List.of(
            "2D E9 F? 4? D0 F8 00 80 81 46 D8 F8 18 00 D0 F8"
        ),

        "x64", List.of(
            "55 41 57 41 56 41 55 41 54 53 50 49 89 fe 48 8b 1f 48 8b 43 30 4c 8b b8 d0 01 00 00 4d 85 ff 74 12 4d 8b a7 90 00 00 00 4d 85 e4 74 4a 49 8b 04 24 eb 46",
            "55 41 57 41 56 41 55 41 54 53 50 49 89 F? 4? 8B ?? 4? 8B 4? 30 4C 8B ?? ?? 0? 00 00 4D 85 ?? 74 1? 4D 8B",
            "55 41 57 41 56 41 55 41 54 53 48 83 EC 18 49 89 FF 48 8B 1F 48 8B 43 30 4C 8B A0 28 02 00 00 4D 85 E4 74",
            "55 41 57 41 56 41 55 41 54 53 48 83 EC 18 49 89 FE 4C 8B 27 49 8B 44 24 30 48 8B 98 D0 01 00 00 48 85 DB",
            "55 41 57 41 56 41 55 41 54 53 48 83 EC 38 C6 02 50 48 8B AF ?? 00 00 00 48 85 ED 74 ?? 48 83 7D 00 00 74 ??" // ssl_crypto_x509_session_verify_cert_chain
        ),

        "x86", List.of(
            "55 89 E5 53 57 56 83 E4 F0 83 EC 20 E8 00 00 00 00 5B 81 C3 2B 79 66 00 8B 7D 08 8B 17 8B 42 18 8B 80 88 01"
        )
    );

    /**
     * Detects the device CPU architecture from system properties.
     *  @return arch: "arm64", "arm", "x64", "x86"
     */
    private static String detectArch(String abi) {

        if (abi.contains("aarch64") || abi.contains("arm64"))
            return "arm64";
        if (abi.contains("arm"))
            return "arm";
        if (abi.contains("x86_64") || abi.contains("amd64"))
            return "x64";
        if (abi.contains("i686") || abi.contains("i386") || abi.contains("x86"))
            return "x86";

        return abi;
    }

    /**
     * Returns byte patterns for the given CPU architecture.
     *
     * @param arch Normalized  CPU architecture name ("arm64", "arm", "x64", "x86")
     * @return Pattern array for the architecture, or null if unsupported.
     */
    private static List<String> matchPattern(String arch) {

        List<String> patterns = PATTERNS.get(arch);

        if (patterns == null) {
            Log.w("[WARN]", "Unsupported architecture: " + arch);
            return null;
        }

        return patterns;
    }

    /** ========== MAIN PATCH FUNCTION ==========
     * Patches libflutter.so directly (in-place) by scanning for patterns,
     * @return true if patched successfully
     *
     * Scan & Patch library
     */
    public static boolean patchLibrary(byte[] bytes, String abi) {

        String arch = detectArch(abi);
        List<String> patterns = matchPattern(arch);

        if (patterns == null) {
            return false;
        }

        byte[] returnZero;
        if ("arm64".equals(arch)) {
            returnZero = RETURN_ZERO_ARM64;
        } else if ("arm".equals(arch)) {
            returnZero = RETURN_ZERO_ARM32;
        } else {
            returnZero = RETURN_ZERO_X86_X64;
        }

        byte[] returnOne;
        if ("arm64".equals(arch)) {
            returnOne = RETURN_ONE_ARM64;
        } else {
            returnOne = RETURN_ONE_X86_X64;
        }

        boolean patched = false;

        for (int idx = 0; idx < patterns.size(); idx++) {
            String pattern = patterns.get(idx);

            List<PatternByte> compiledPattern = compilePattern(pattern);
            List<Integer> offsets = scanForPattern(bytes, compiledPattern);

            // Last pattern (index 4) = ssl_crypto_x509_session_verify_cert_chain ➤ return 1
            byte[] patchBytes;
            if (idx == 4 && ("arm64".equals(arch) || "x64".equals(arch))) {
                patchBytes = returnOne;
            } else {
                patchBytes = returnZero;
            }

            for (int offset : offsets) {

                Log.i("[INFO]", "Matched Pattern - " + idx + " ➢ " + pattern);

                try {
                    System.arraycopy(
                        patchBytes,
                        0,
                        bytes,
                        offset,
                        patchBytes.length
                    );

                    patched = true;

                    Log.i("[INFO]", "Patched ssl_verify_peer_cert at offset: 0x" + Integer.toHexString(offset));
                    Log.i("[INFO]", "[✓] Patched library successfully.");

                } catch (Exception e) {
                    Log.e(
                        "[ERROR]",
                        "Failed to patch at offset 0x" + Integer.toHexString(offset) + ": " + e.getMessage()
                    );
                }
            }

            // Stop after first matching pattern
            if (patched) break;
        }

        if (!patched) {
            Log.w("[WARN]", "No ssl_verify_peer_cert pattern matched !");
            Log.w("[WARN]", "Possibly not a Flutter app or patterns outdated !");
        }

        return patched;
    }

    /* ========== Pattern Matching Engine ========== */
    
    private static final class PatternByte {

        final int value;
        final int mask;

        PatternByte(int value, int mask) {
            this.value = value;
            this.mask = mask;
        }
    }

    /*
     * A compiled pattern entry: value to match and mask.
     * If mask bit is 0, that nibble is a wildcard.
     *
     * Compiles a hex pattern string like "F? 0F 1C F8 ?? 5? 01 A9"
     * into a list of PatternByte with masks.
     *
     * - "FF" → value=0xFF, mask=0xFF (exact match)
     * - "F?" → value=0xF0, mask=0xF0 (high nibble must be F, low nibble wildcard)
     * - "??" → value=0x00, mask=0x00 (full wildcard)
     * - "?F" → value=0x0F, mask=0x0F (high nibble wildcard, low nibble must be F)
     */
    private static List<PatternByte> compilePattern(String pattern) {

        String[] tokens = pattern.trim().split("\\s+");

        for (String token : tokens) {
            if (token.length() != 2) {
                throw new IllegalArgumentException("Invalid pattern token: " + token);
            }
        }

        List<PatternByte> compiled = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            int value = 0;
            int mask = 0;

            if (token.charAt(0) != '?') {
                value |= Character.digit(token.charAt(0), 16) << 4;
                mask |= 0xF0;
            }

            if (token.charAt(1) != '?') {
                value |= Character.digit(token.charAt(1), 16);
                mask |= 0x0F;
            }

            compiled.add(new PatternByte(value, mask));
        }

        return compiled;
    }

    /**
     * Scans a byte array for all occurrences of a compiled pattern.
     * Returns a list of offsets where the pattern matches.
     */
    private static List<Integer> scanForPattern(byte[] data, List<PatternByte> pattern) {

        List<Integer> matches = new ArrayList<>();

        if (pattern.isEmpty() || data.length < pattern.size()) {
            return matches;
        }

        int maxOffset = data.length - pattern.size();

        search:
        for (int offset = 0; offset <= maxOffset; offset++) {
            for (int index = 0; index < pattern.size(); index++) {
                int value = data[offset + index] & 0xFF;
                PatternByte patternByte = pattern.get(index);

                if ((value & patternByte.mask) != patternByte.value) {
                    continue search;
                }
            }
            matches.add(offset);
        }

        return matches;
    }
}