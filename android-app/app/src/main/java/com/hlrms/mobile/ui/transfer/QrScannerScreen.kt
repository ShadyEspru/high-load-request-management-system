package com.hlrms.mobile.ui.transfer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.hlrms.mobile.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

@Composable
fun QrScannerScreen(
    onBackClick: () -> Unit,
    onSearchByIdClick: () -> Unit,
    onTransferIdDetected: (
        transferId: String
    ) -> Unit
) {

    val context =
        LocalContext.current

    val cameraPermissionRequiredMessage =
        stringResource(
            R.string.qr_camera_permission_required
        )

    val invalidQrMessage =
        stringResource(
            R.string.qr_invalid_transfer
        )

    var hasCameraPermission by remember {

        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var acceptingScans by remember {
        mutableStateOf(true)
    }

    /*
     * لا نبدأ CameraX في نفس frame الذي تظهر فيه الشاشة.
     * نعطي Compose وقتًا قصيرًا لرسم الـ UI أولًا.
     */
    var cameraReady by remember {
        mutableStateOf(false)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->

            hasCameraPermission =
                granted

            if (!granted) {
                errorMessage =
                    cameraPermissionRequiredMessage
            }
        }

    LaunchedEffect(Unit) {

        if (!hasCameraPermission) {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    LaunchedEffect(
        hasCameraPermission
    ) {

        cameraReady =
            false

        if (
            hasCameraPermission
        ) {

            /*
             * الشاشة تُرسم أولًا،
             * وبعدها تبدأ تهيئة CameraX.
             */
            delay(
                150L
            )

            cameraReady =
                true
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
    ) {

        if (hasCameraPermission) {

            if (
                cameraReady
            ) {

                QrCameraPreview(
                    modifier =
                        Modifier.fillMaxSize(),

                    onQrDetected = { rawValue ->

                    if (acceptingScans) {

                        val transferId =
                            TransferQrParser
                                .extractTransferId(
                                    rawValue
                                )

                        if (transferId == null) {

                            errorMessage =
                                invalidQrMessage

                        } else {

                            acceptingScans =
                                false

                            errorMessage =
                                null

                            onTransferIdDetected(
                                transferId
                            )
                        }
                    }
                }
                )
            }

            ScannerOverlay(
                errorMessage =
                    errorMessage,

                onBackClick =
                    onBackClick,

                onSearchByIdClick =
                    onSearchByIdClick
            )

        } else {

            PermissionContent(
                errorMessage =
                    errorMessage,

                onBackClick =
                    onBackClick,

                onRequestPermission = {

                    permissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }
            )
        }
    }
}

@Composable
private fun ScannerOverlay(
    errorMessage: String?,
    onBackClick: () -> Unit,
    onSearchByIdClick: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color =
                            Color.Black.copy(
                                alpha = 0.45f
                            ),

                        shape =
                            RoundedCornerShape(
                                18.dp
                            )
                    )
                    .padding(
                        horizontal = 6.dp
                    ),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick =
                    onBackClick,

                colors =
                    ButtonDefaults
                        .textButtonColors(
                            contentColor =
                                Color.White
                        )
            ) {

                Text(
                    text = stringResource(R.string.back)
                )
            }

            Text(
                text = stringResource(R.string.send_transfer),
                color = Color.White,
                fontWeight =
                    FontWeight.Bold
            )

            TextButton(
                onClick =
                    onSearchByIdClick,

                colors =
                    ButtonDefaults
                        .textButtonColors(
                            contentColor =
                                Color.White
                        )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string.search_by_transfer_id
                        )
                )
            }
        }

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        QrGuideFrame(
            modifier =
                Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .align(
                        Alignment.CenterHorizontally
                    )
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text =
                stringResource(R.string.qr_scan_instruction),

            modifier =
                Modifier.fillMaxWidth(),

            color =
                Color.White,

            textAlign =
                TextAlign.Center,

            style =
                MaterialTheme
                    .typography
                    .titleMedium,

            fontWeight =
                FontWeight.SemiBold
        )

        if (errorMessage != null) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    errorMessage,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color =
                                Color.Black.copy(
                                    alpha = 0.6f
                                ),

                            shape =
                                RoundedCornerShape(
                                    12.dp
                                )
                        )
                        .padding(12.dp),

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                textAlign =
                    TextAlign.Center
            )
        }

        Spacer(
            modifier =
                Modifier.weight(1f)
        )
    }
}

@Composable
private fun QrGuideFrame(
    modifier: Modifier
) {

    val guideColor =
        MaterialTheme
            .colorScheme
            .primary

    Canvas(
        modifier =
            modifier
    ) {

        val outerStroke =
            4.dp.toPx()

        val gridStroke =
            1.dp.toPx()

        val cornerRadius =
            22.dp.toPx()

        drawRoundRect(
            color =
                guideColor,

            cornerRadius =
                CornerRadius(
                    cornerRadius,
                    cornerRadius
                ),

            style =
                Stroke(
                    width =
                        outerStroke
                )
        )

        val oneThirdX =
            size.width / 3f

        val twoThirdX =
            size.width * 2f / 3f

        val oneThirdY =
            size.height / 3f

        val twoThirdY =
            size.height * 2f / 3f

        val gridColor =
            Color.White.copy(
                alpha = 0.45f
            )

        drawLine(
            color =
                gridColor,

            start =
                androidx.compose.ui.geometry.Offset(
                    oneThirdX,
                    0f
                ),

            end =
                androidx.compose.ui.geometry.Offset(
                    oneThirdX,
                    size.height
                ),

            strokeWidth =
                gridStroke
        )

        drawLine(
            color =
                gridColor,

            start =
                androidx.compose.ui.geometry.Offset(
                    twoThirdX,
                    0f
                ),

            end =
                androidx.compose.ui.geometry.Offset(
                    twoThirdX,
                    size.height
                ),

            strokeWidth =
                gridStroke
        )

        drawLine(
            color =
                gridColor,

            start =
                androidx.compose.ui.geometry.Offset(
                    0f,
                    oneThirdY
                ),

            end =
                androidx.compose.ui.geometry.Offset(
                    size.width,
                    oneThirdY
                ),

            strokeWidth =
                gridStroke
        )

        drawLine(
            color =
                gridColor,

            start =
                androidx.compose.ui.geometry.Offset(
                    0f,
                    twoThirdY
                ),

            end =
                androidx.compose.ui.geometry.Offset(
                    size.width,
                    twoThirdY
                ),

            strokeWidth =
                gridStroke
        )
    }
}

@Composable
private fun PermissionContent(
    errorMessage: String?,
    onBackClick: () -> Unit,
    onRequestPermission: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.Start
        ) {

            TextButton(
                onClick =
                    onBackClick,

                colors =
                    ButtonDefaults
                        .textButtonColors(
                            contentColor =
                                Color.White
                        )
            ) {

                Text(stringResource(R.string.back))
            }
        }

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        Text(
            text =
                errorMessage
                    ?: stringResource(R.string.qr_camera_permission_needed),

            color =
                Color.White,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Button(
            onClick =
                onRequestPermission
        ) {

            Text(
                stringResource(R.string.allow_camera)
            )
        }

        Spacer(
            modifier =
                Modifier.weight(1f)
        )
    }
}

@SuppressLint(
    "UnsafeOptInUsageError"
)
@Composable
private fun QrCameraPreview(
    modifier: Modifier,
    onQrDetected: (
        value: String
    ) -> Unit
) {

    val context =
        LocalContext.current

    val lifecycleOwner =
        LocalLifecycleOwner.current

    val previewView =
        remember {

            PreviewView(
                context
            ).apply {

                implementationMode =
                    PreviewView
                        .ImplementationMode
                        .COMPATIBLE

                scaleType =
                    PreviewView
                        .ScaleType
                        .FILL_CENTER
            }
        }

    val analyzerExecutor =
        remember {

            Executors
                .newSingleThreadExecutor()
        }

    val analysisBusy =
        remember {

            AtomicBoolean(false)
        }

    val scanner =
        remember {

            val options =
                BarcodeScannerOptions
                    .Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE
                    )
                    .build()

            BarcodeScanning
                .getClient(
                    options
                )
        }

    AndroidView(
        factory = {
            previewView
        },

        modifier =
            modifier
    )

    DisposableEffect(
        lifecycleOwner
    ) {

        val cameraProviderFuture =
            ProcessCameraProvider
                .getInstance(
                    context
                )

        val mainExecutor =
            ContextCompat
                .getMainExecutor(
                    context
                )

        /*
         * مهم خصوصًا إذا خرج المستخدم من الشاشة
         * والكاميرا ما زالت في مرحلة التهيئة.
         */
        var disposed =
            false

        var activeCameraProvider:
                ProcessCameraProvider? =
            null

        var activeImageAnalysis:
                ImageAnalysis? =
            null

        cameraProviderFuture
            .addListener(
                cameraListener@{

                    /*
                     * قد تكتمل عملية getInstance بعد مغادرة الشاشة.
                     * في هذه الحالة لا نربط الكاميرا أصلًا.
                     */
                    if (
                        disposed
                    ) {

                        return@cameraListener
                    }

                    val cameraProvider =
                        cameraProviderFuture
                            .get()

                    activeCameraProvider =
                        cameraProvider

                    val preview =
                        Preview
                            .Builder()
                            .build()
                            .also {

                                it.surfaceProvider =
                                    previewView
                                        .surfaceProvider
                            }

                    val imageAnalysis =
                        ImageAnalysis
                            .Builder()
                            .setBackpressureStrategy(
                                ImageAnalysis
                                    .STRATEGY_KEEP_ONLY_LATEST
                            )
                            .build()

                    activeImageAnalysis =
                        imageAnalysis

                    imageAnalysis
                        .setAnalyzer(
                            analyzerExecutor
                        ) { imageProxy ->

                            /*
                             * لا نعالج أي frame بعد مغادرة الشاشة.
                             */
                            if (
                                disposed
                            ) {

                                imageProxy.close()

                                return@setAnalyzer
                            }

                            if (
                                !analysisBusy
                                    .compareAndSet(
                                        false,
                                        true
                                    )
                            ) {

                                imageProxy.close()

                                return@setAnalyzer
                            }

                            val mediaImage =
                                imageProxy.image

                            if (
                                mediaImage == null
                            ) {

                                analysisBusy.set(
                                    false
                                )

                                imageProxy.close()

                                return@setAnalyzer
                            }

                            val inputImage =
                                InputImage
                                    .fromMediaImage(
                                        mediaImage,
                                        imageProxy
                                            .imageInfo
                                            .rotationDegrees
                                    )

                            scanner
                                .process(
                                    inputImage
                                )
                                .addOnSuccessListener {
                                        barcodes ->

                                    if (
                                        disposed
                                    ) {

                                        return@addOnSuccessListener
                                    }

                                    val rawValue =
                                        barcodes
                                            .firstOrNull {
                                                it.format ==
                                                        Barcode
                                                            .FORMAT_QR_CODE
                                            }
                                            ?.rawValue

                                    if (
                                        !rawValue
                                            .isNullOrBlank()
                                    ) {

                                        mainExecutor.execute {

                                            if (
                                                !disposed
                                            ) {

                                                onQrDetected(
                                                    rawValue
                                                )
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener {

                                    analysisBusy.set(
                                        false
                                    )

                                    imageProxy.close()
                                }
                        }

                    /*
                     * تأكيد إضافي قبل bind.
                     */
                    if (
                        disposed
                    ) {

                        imageAnalysis
                            .clearAnalyzer()

                        return@cameraListener
                    }

                    cameraProvider
                        .unbindAll()

                    cameraProvider
                        .bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector
                                .DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                },

                mainExecutor
            )

        onDispose {

            disposed =
                true

            activeImageAnalysis
                ?.clearAnalyzer()

            runCatching {

                activeCameraProvider
                    ?.unbindAll()
            }

            /*
             * إذا اكتمل Future ولكن المرجع لم يُخزن بعد،
             * نحرر الـ provider احتياطيًا.
             */
            if (
                cameraProviderFuture
                    .isDone
            ) {

                runCatching {

                    cameraProviderFuture
                        .get()
                        .unbindAll()
                }
            }
        }
    }

    DisposableEffect(Unit) {

        onDispose {

            scanner.close()

            analyzerExecutor
                .shutdown()
        }
    }
}