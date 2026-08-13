package com.hlrms.mobile.ui.receive

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

object QrCodeGenerator {

    fun generate(
        content: String,
        size: Int = 512
    ): Bitmap {

        val matrix =
            MultiFormatWriter()
                .encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
                )

        /*
         * نملأ مصفوفة كاملة أولًا ثم نرسلها
         * إلى Bitmap دفعة واحدة.
         *
         * هذا أسرع بكثير من setPixel()
         * لنصف مليون مرة تقريبًا.
         */
        val pixels =
            IntArray(
                size * size
            )

        var index = 0

        for (y in 0 until size) {

            for (x in 0 until size) {

                pixels[index++] =
                    if (matrix[x, y]) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
            }
        }

        return Bitmap
            .createBitmap(
                size,
                size,
                Bitmap.Config.RGB_565
            )
            .apply {

                setPixels(
                    pixels,
                    0,
                    size,
                    0,
                    0,
                    size,
                    size
                )
            }
    }
}
