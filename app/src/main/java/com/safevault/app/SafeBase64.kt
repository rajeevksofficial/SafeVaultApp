package com.safevault.app

object SafeBase64 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val DECODE_TABLE = IntArray(256) { -1 }

    init {
        for (i in ALPHABET.indices) {
            DECODE_TABLE[ALPHABET[i].code] = i
        }
    }

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val out = StringBuilder((data.size + 2) / 3 * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i++].toInt() and 0xFF
            val b1 = if (i < data.size) data[i++].toInt() and 0xFF else -1
            val b2 = if (i < data.size) data[i++].toInt() and 0xFF else -1

            out.append(ALPHABET[b0 shr 2])
            if (b1 != -1) {
                out.append(ALPHABET[((b0 and 0x03) shl 4) or (b1 shr 4)])
                if (b2 != -1) {
                    out.append(ALPHABET[((b1 and 0x0F) shl 2) or (b2 shr 6)])
                    out.append(ALPHABET[b2 and 0x3F])
                } else {
                    out.append(ALPHABET[(b1 and 0x0F) shl 2])
                    out.append('=')
                }
            } else {
                out.append(ALPHABET[(b0 and 0x03) shl 4])
                out.append("==")
            }
        }
        return out.toString()
    }

    fun decode(input: String): ByteArray {
        val clean = input.trim()
        if (clean.isEmpty()) return ByteArray(0)

        var padCount = 0
        if (clean.endsWith("==")) padCount = 2
        else if (clean.endsWith("=")) padCount = 1

        val len = (clean.length * 3 / 4) - padCount
        val out = ByteArray(len)
        var outIdx = 0
        var i = 0

        while (i < clean.length) {
            val c0 = clean[i++]
            val c1 = clean[i++]
            val c2 = if (i < clean.length) clean[i++] else '='
            val c3 = if (i < clean.length) clean[i++] else '='

            val v0 = if (c0.code < 256) DECODE_TABLE[c0.code] else -1
            val v1 = if (c1.code < 256) DECODE_TABLE[c1.code] else -1
            val v2 = if (c2 != '=' && c2.code < 256) DECODE_TABLE[c2.code] else 0
            val v3 = if (c3 != '=' && c3.code < 256) DECODE_TABLE[c3.code] else 0

            if (v0 == -1 || v1 == -1) break

            val triple = (v0 shl 18) or (v1 shl 12) or (v2 shl 6) or v3

            if (outIdx < len) out[outIdx++] = ((triple shr 16) and 0xFF).toByte()
            if (c2 != '=' && outIdx < len) out[outIdx++] = ((triple shr 8) and 0xFF).toByte()
            if (c3 != '=' && outIdx < len) out[outIdx++] = (triple and 0xFF).toByte()
        }
        return out
    }
}
