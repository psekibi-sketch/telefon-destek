package com.example.sip

/**
 * ITU-T G.711 Audio Codec (PCMA - A-law ve PCMU - µ-law)
 * VoIP / SIP standartlarında 8000 Hz 16-bit PCM ses akışını sıkıştırmak
 * ve çözmek için donanım bağımsız, saf Kotlin implementasyonu.
 */
object G711Codec {

    private val aLawToLinear = ShortArray(256)
    private val uLawToLinear = ShortArray(256)

    init {
        // A-law to Linear PCM tablosu başlatma
        for (i in 0 until 256) {
            var input = i xor 0x55
            val sign = input and 0x80
            val exponent = (input and 0x70) shr 4
            var mantissa = input and 0x0F
            var value = if (exponent == 0) {
                (mantissa shl 4) + 8
            } else {
                ((mantissa shl 4) + 0x108) shl (exponent - 1)
            }
            if (sign == 0) value = -value
            aLawToLinear[i] = value.toShort()
        }

        // µ-law to Linear PCM tablosu başlatma
        for (i in 0 until 256) {
            val input = i.inv() and 0xFF
            val sign = input and 0x80
            val exponent = (input and 0x70) shr 4
            val mantissa = input and 0x0F
            var value = ((mantissa shl 3) + 0x84) shl exponent
            value -= 0x84
            if (sign != 0) value = -value
            uLawToLinear[i] = value.toShort()
        }
    }

    /**
     * 16-bit Linear PCM örneğini 8-bit PCMA (A-law, RTP Payload 8) baytına dönüştürür.
     */
    fun linearToAlaw(pcmVal: Short): Byte {
        var pcm = pcmVal.toInt()
        val mask: Int
        if (pcm >= 0) {
            mask = 0xD5
        } else {
            mask = 0x55
            pcm = -pcm - 1
        }
        if (pcm > 32767) pcm = 32767

        var seg = 8
        var temp = pcm shr 4
        while (seg > 0 && (temp and 0x100) == 0) {
            seg--
            temp = temp shl 1
        }

        val aval: Int = if (seg >= 8) {
            0x7F
        } else if (seg == 0) {
            (pcm shr 4) and 0x0F
        } else {
            (seg shl 4) or ((pcm shr (seg + 3)) and 0x0F)
        }
        return (aval xor mask).toByte()
    }

    /**
     * 8-bit PCMA (A-law) baytını 16-bit Linear PCM değerine dönüştürür.
     */
    fun alawToLinear(alawByte: Byte): Short {
        val index = alawByte.toInt() and 0xFF
        return aLawToLinear[index]
    }

    /**
     * 16-bit Linear PCM örneğini 8-bit PCMU (µ-law, RTP Payload 0) baytına dönüştürür.
     */
    fun linearToUlaw(pcmVal: Short): Byte {
        var pcm = pcmVal.toInt()
        val sign = (pcm shr 8) and 0x80
        if (sign != 0) pcm = -pcm
        if (pcm > 32635) pcm = 32635
        pcm += 0x84

        var exponent = 7
        var expMask = 0x4000
        while ((pcm and expMask) == 0 && exponent > 0) {
            exponent--
            expMask = expMask shr 1
        }
        val mantissa = (pcm shr (exponent + 3)) and 0x0F
        val ulaw = (sign or (exponent shl 4) or mantissa).inv()
        return ulaw.toByte()
    }

    /**
     * 8-bit PCMU (µ-law) baytını 16-bit Linear PCM değerine dönüştürür.
     */
    fun ulawToLinear(ulawByte: Byte): Short {
        val index = ulawByte.toInt() and 0xFF
        return uLawToLinear[index]
    }

    /**
     * Gelen RTP ses bayt dizisini PCM 16-bit örneklerine dönüştürür.
     */
    fun decodeRtpPayload(
        payload: ByteArray,
        offset: Int,
        length: Int,
        payloadType: Int,
        outputPcm: ShortArray
    ): Int {
        val count = minOf(length, outputPcm.size)
        if (payloadType == 8) {
            // PCMA (A-law)
            for (i in 0 until count) {
                outputPcm[i] = alawToLinear(payload[offset + i])
            }
        } else {
            // PCMU (µ-law) veya varsayılan
            for (i in 0 until count) {
                outputPcm[i] = ulawToLinear(payload[offset + i])
            }
        }
        return count
    }

    /**
     * Mikrofondan okunan 160 PCM 16-bit örneğini RTP ses baytlarına kodlar.
     */
    fun encodeRtpPayload(
        inputPcm: ShortArray,
        pcmOffset: Int,
        pcmCount: Int,
        payloadType: Int,
        outputPayload: ByteArray,
        outputOffset: Int
    ) {
        val count = minOf(pcmCount, outputPayload.size - outputOffset)
        if (payloadType == 8) {
            for (i in 0 until count) {
                outputPayload[outputOffset + i] = linearToAlaw(inputPcm[pcmOffset + i])
            }
        } else {
            for (i in 0 until count) {
                outputPayload[outputOffset + i] = linearToUlaw(inputPcm[pcmOffset + i])
            }
        }
    }
}
