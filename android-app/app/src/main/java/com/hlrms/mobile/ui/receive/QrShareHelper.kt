package com.hlrms.mobile.ui.receive

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object QrShareHelper {

    fun share(
        context: Context,
        qrBitmap: Bitmap,
        transferId: String,
        qrContent: String
    ) {

        val shareDirectory =
            File(
                context.cacheDir,
                "shared_qr"
            )

        if (
            !shareDirectory.exists()
        ) {

            shareDirectory.mkdirs()
        }

        val imageFile =
            File(
                shareDirectory,
                "hlrms_cash_$transferId.png"
            )

        FileOutputStream(
            imageFile
        ).use { outputStream ->

            qrBitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                outputStream
            )
        }

        val imageUri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

        val shareText =
            """
            HLRMS Cash
            
            معرف التحويل:
            $transferId
            
            $qrContent
            """.trimIndent()

        val shareIntent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    "image/png"

                putExtra(
                    Intent.EXTRA_STREAM,
                    imageUri
                )

                putExtra(
                    Intent.EXTRA_TEXT,
                    shareText
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        context.startActivity(
            Intent.createChooser(
                shareIntent,
                "مشاركة رمز الاستقبال"
            )
        )
    }
}