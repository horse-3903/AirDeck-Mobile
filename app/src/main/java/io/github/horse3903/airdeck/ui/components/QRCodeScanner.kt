package io.github.horse3903.airdeck.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import io.github.horse3903.airdeck.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

sealed class QRScanResult {
    data class Success(val content: String) : QRScanResult()
    data class Error(val message: String) : QRScanResult()
    object Cancelled : QRScanResult()
    object PermissionDenied : QRScanResult()
}

/**
 * Returns a function that opens a full screen QR scanner (after asking for camera permission).
 */
@Composable
fun rememberQRCodeScanner(
    onResult: (QRScanResult) -> Unit
): () -> Unit {
    val context = LocalContext.current
    var showScanner by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        } else {
            onResult(QRScanResult.PermissionDenied)
        }
    }

    if (showScanner) {
        QRScannerDialog(
            onResult = { result ->
                showScanner = false
                onResult(result)
            }
        )
    }

    return {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            showScanner = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
private fun QRScannerDialog(onResult: (QRScanResult) -> Unit) {
    Dialog(
        onDismissRequest = { onResult(QRScanResult.Cancelled) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        QRScannerContent(onResult = onResult)
    }
}

private val scannerHints = mapOf(
    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    DecodeHintType.TRY_HARDER to true
)

private data class DetectedCode(val text: String, val bounds: Rect)

@Composable
private fun QRScannerContent(onResult: (QRScanResult) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var torchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var detected by remember { mutableStateOf<DetectedCode?>(null) }
    val locked = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val sizeRef = remember { mutableStateOf(IntSize.Zero) }
    sizeRef.value = viewSize

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                )
                .build()
            val reader = MultiFormatReader().apply { setHints(scannerHints) }
            analysis.setAnalyzer(executor) { image ->
                try {
                    if (locked.get()) return@setAnalyzer
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)
                    val source = PlanarYUVLuminanceSource(
                        data, plane.rowStride, image.height,
                        0, 0, image.width, image.height, false
                    )
                    val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
                    val points = result.resultPoints
                    val view = sizeRef.value
                    if (!locked.getAndSet(true)) {
                        val iw = image.width.toFloat()
                        val ih = image.height.toFloat()
                        val rotation = image.imageInfo.rotationDegrees
                        val (rw, rh) = if (rotation == 90 || rotation == 270) ih to iw else iw to ih
                        val mapped = points.map { p ->
                            when (rotation) {
                                90 -> Offset(ih - p.y, p.x)
                                180 -> Offset(iw - p.x, ih - p.y)
                                270 -> Offset(p.y, iw - p.x)
                                else -> Offset(p.x, p.y)
                            }
                        }
                        val scale = max(view.width / rw, view.height / rh)
                        val dx = (view.width - rw * scale) / 2f
                        val dy = (view.height - rh * scale) / 2f
                        val minX = mapped.minOf { it.x }
                        val maxX = mapped.maxOf { it.x }
                        val minY = mapped.minOf { it.y }
                        val maxY = mapped.maxOf { it.y }
                        val side = max(maxX - minX, maxY - minY) * 1.34f * scale
                        val cx = (minX + maxX) / 2f * scale + dx
                        val cy = (minY + maxY) / 2f * scale + dy
                        detected = DetectedCode(
                            result.text,
                            Rect(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2)
                        )
                    }
                } catch (_: Exception) {
                    // No code in this frame
                } finally {
                    reader.reset()
                    image.close()
                }
            }
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }

    LaunchedEffect(torchOn, camera) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(torchOn)
    }

    LaunchedEffect(detected) {
        detected?.let {
            delay(700)
            onResult(QRScanResult.Success(it.text))
        }
    }

    val noCodeMessage = stringResource(R.string.qr_scan_gallery_none)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.Default) { decodeFromImage(context, uri) }
                if (text != null) {
                    locked.set(true)
                    onResult(QRScanResult.Success(text))
                } else {
                    Toast.makeText(context, noCodeMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewSize = it }
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        ScannerFrame(viewSize = viewSize, target = detected?.bounds)

        Text(
            text = stringResource(R.string.qr_scan_title),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 120.dp),
            textAlign = TextAlign.Center,
            style = TextStyle(
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(96.dp)
        ) {
            ScannerRoundButton(
                iconRes = R.drawable.ic_flashlight,
                description = stringResource(R.string.qr_scan_torch),
                active = torchOn,
                onClick = { torchOn = !torchOn }
            )
            ScannerRoundButton(
                iconRes = R.drawable.ic_image,
                description = stringResource(R.string.qr_scan_gallery),
                active = false,
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
    }
}

@Composable
private fun ScannerRoundButton(
    iconRes: Int,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = if (active) Color.Black else Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * The four rounded corner brackets. They rest in the largest centred square that leaves an edge margin,
 * and glide onto a code once found.
 */
@Composable
private fun ScannerFrame(viewSize: IntSize, target: Rect?) {
    val width = viewSize.width.toFloat()
    val height = viewSize.height.toFloat()
    val edge = with(LocalDensity.current) { 24.dp.toPx() }
    val side = (min(width, height) - 2 * edge).coerceAtLeast(0f)
    val resting = Rect(
        left = (width - side) / 2f,
        top = (height - side) / 2f,
        right = (width + side) / 2f,
        bottom = (height + side) / 2f
    )
    val frame by animateRectAsState(
        targetValue = target ?: resting,
        animationSpec = tween(durationMillis = 380),
        label = "scannerFrame"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val arm = 30.dp.toPx()
        val radius = 14.dp.toPx()
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val shortSide = min(frame.width, frame.height)
        val armLength = min(arm, shortSide / 3f)
        drawBracket(frame.left, frame.top, 1f, 1f, armLength, radius, stroke)
        drawBracket(frame.right, frame.top, -1f, 1f, armLength, radius, stroke)
        drawBracket(frame.left, frame.bottom, 1f, -1f, armLength, radius, stroke)
        drawBracket(frame.right, frame.bottom, -1f, -1f, armLength, radius, stroke)
    }
}

private fun DrawScope.drawBracket(
    cornerX: Float,
    cornerY: Float,
    dirX: Float,
    dirY: Float,
    arm: Float,
    radius: Float,
    stroke: Stroke
) {
    val path = Path().apply {
        moveTo(cornerX, cornerY + arm * dirY)
        lineTo(cornerX, cornerY + radius * dirY)
        quadraticTo(cornerX, cornerY, cornerX + radius * dirX, cornerY)
        lineTo(cornerX + arm * dirX, cornerY)
    }
    drawPath(path, Color.White, style = stroke)
}

private fun decodeFromImage(context: android.content.Context, uri: Uri): String? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 2000 || bounds.outHeight / sample > 2000) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        MultiFormatReader().apply { setHints(scannerHints) }
            .decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            .text
    } catch (_: Exception) {
        null
    }
}
