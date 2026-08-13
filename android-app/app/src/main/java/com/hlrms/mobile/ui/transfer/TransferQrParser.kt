package com.hlrms.mobile.ui.transfer

object TransferQrParser {

    private val transferQrPattern =
        Regex(
            pattern =
                "^hlrms://transfer/([A-HJ-NP-Z2-9]{16})$",

            option =
                RegexOption.IGNORE_CASE
        )

    fun extractTransferId(
        qrContent: String
    ): String? {

        val match =
            transferQrPattern.matchEntire(
                qrContent.trim()
            )
                ?: return null

        return match
            .groupValues[1]
            .uppercase()
    }
}