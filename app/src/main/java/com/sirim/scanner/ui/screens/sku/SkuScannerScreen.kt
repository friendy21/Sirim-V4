package com.sirim.scanner.ui.screens.sku

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirim.scanner.data.ocr.BarcodeAnalyzer
import com.sirim.scanner.data.repository.SirimRepository
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkuScannerScreen(
    onBack: () -> Unit,
    onRecordSaved: (Long) -> Unit,
    repository: SirimRepository,
    analyzer: BarcodeAnalyzer,
    appScope: CoroutineScope
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val viewModel: SkuScannerViewModel = viewModel(
        factory = SkuScannerViewModel.Factory(repository, analyzer, appScope)
    )
    val status by viewModel.status.collectAsState()
    val batchState by viewModel.batchUiState.collectAsState()
    val lastSavedId by viewModel.lastSavedId.collectAsState()
    val hasCameraPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(lastSavedId) {
        lastSavedId?.let(onRecordSaved)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SKU Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SkuStatusCard(status = status)
            SkuBatchControls(
                state = batchState,
                onToggle = viewModel::setBatchMode,
                onSave = viewModel::saveBatch,
                onClear = viewModel::clearBatchQueue
            )
            if (hasCameraPermission.value) {
                SkuCameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    lifecycleOwner = lifecycleOwner,
                    viewModel = viewModel,
                    status = status
                )
            } else {
                Text(
                    text = "Camera permission is required to scan barcodes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SkuStatusCard(status: SkuScanStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = status.message,
                style = MaterialTheme.typography.titleMedium,
                color = when (status.state) {
                    SkuScanState.Saved -> MaterialTheme.colorScheme.primary
                    SkuScanState.Duplicate -> MaterialTheme.colorScheme.error
                    SkuScanState.Error -> MaterialTheme.colorScheme.error
                    SkuScanState.Batch -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            status.barcode?.let { barcode ->
                Text(
                    text = barcode,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            status.format?.let { format ->
                Text(
                    text = format,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SkuBatchControls(
    state: SkuBatchUiState,
    onToggle: (Boolean) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Batch mode", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Queued ${state.queued.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.enabled, onCheckedChange = onToggle)
            }

            if (state.queued.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.queued.takeLast(8)) { item ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(item.barcode, style = MaterialTheme.typography.bodyMedium)
                                item.format?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text("Save batch")
                    }
                    TextButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                        Text("Clear queue")
                    }
                }
            }
        }
    }
}

@Composable
private fun SkuCameraPreview(
    modifier: Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    viewModel: SkuScannerViewModel,
    status: SkuScanStatus
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val camera = remember { mutableStateOf<Camera?>(null) }
    val flashEnabled = rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(executor) { image ->
                        viewModel.analyzeImage(image)
                    }
                }
            val boundCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
            camera.value = boundCamera
            enableSkuTapToFocus(previewView, boundCamera.cameraControl, previewView.meteringPointFactory)
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    LaunchedEffect(flashEnabled.value) {
        camera.value?.cameraControl?.enableTorch(flashEnabled.value)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            SkuScannerOverlay(status = status)
            IconButton(
                onClick = { flashEnabled.value = !flashEnabled.value },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = if (flashEnabled.value) Icons.Rounded.Bolt else Icons.Rounded.FlashOff,
                    contentDescription = if (flashEnabled.value) "Disable flash" else "Enable flash"
                )
            }
        }
    }
}

@Composable
private fun SkuScannerOverlay(status: SkuScanStatus) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val padding = 64.dp.toPx()
        val width = size.width - padding * 2
        val height = size.height - padding * 2
        val color = when (status.state) {
            SkuScanState.Saved -> Color(0xFF4CAF50)
            SkuScanState.Duplicate -> Color(0xFFF44336)
            SkuScanState.Error -> Color(0xFFF44336)
            SkuScanState.Batch -> Color(0xFF03A9F4)
            else -> Color.White.copy(alpha = 0.65f)
        }
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(width, height),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun enableSkuTapToFocus(
    previewView: PreviewView,
    cameraControl: androidx.camera.core.CameraControl,
    meteringPointFactory: MeteringPointFactory
) {
    previewView.afterMeasured {
        setOnTouchListener { _: View, event: MotionEvent ->
            if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener true
            val point = meteringPointFactory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            cameraControl.startFocusAndMetering(action)
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            true
        }
    }
}

private fun View.afterMeasured(block: View.() -> Unit) {
    if (width > 0 && height > 0) {
        block()
    } else {
        addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (right - left > 0 && bottom - top > 0) {
                    removeOnLayoutChangeListener(this)
                    block()
                }
            }
        })
    }
}
