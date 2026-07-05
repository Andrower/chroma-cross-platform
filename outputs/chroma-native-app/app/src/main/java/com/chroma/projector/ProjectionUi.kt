package com.chroma.projector

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas

@Composable
fun ProjectionApp(vm: ProjectionViewModel) {
    MaterialTheme {
        when (vm.launchMode) {
            LaunchMode.Choose -> ModeChooser(
                onControl = vm::chooseControlMode,
                onReceiver = vm::chooseReceiverMode
            )

            LaunchMode.Control -> ControlScreen(vm)
            LaunchMode.Receiver -> ReceiverScreen(vm)
        }
    }
}

@Composable
private fun ModeChooser(onControl: () -> Unit, onReceiver: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("选择进入模式", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))
            ElevatedButton(onClick = onControl, modifier = Modifier.fillMaxWidth()) {
                Text("控制端")
            }
            Spacer(Modifier.height(12.dp))
            ElevatedButton(onClick = onReceiver, modifier = Modifier.fillMaxWidth()) {
                Text("被控端")
            }
        }
    }
}

@Composable
private fun ControlScreen(vm: ProjectionViewModel) {
    val state = vm.editedState
    val devices = vm.connectedDevices
    val selectedId = vm.selectedDeviceId
    val selectedLabel = devices.firstOrNull { it.id == selectedId }?.name ?: "未选择设备"
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("控制端", style = MaterialTheme.typography.headlineMedium)
                    Text(vm.status, style = MaterialTheme.typography.bodyMedium)
                    Text("当前目标：$selectedLabel", style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("在线设备", style = MaterialTheme.typography.titleMedium)
                        if (devices.isEmpty()) {
                            Text("暂无投放端在线", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            devices.forEach { device ->
                                val selected = device.id == selectedId
                                FilterChip(
                                    selected = selected,
                                    onClick = { vm.selectDevice(device.id) },
                                    label = {
                                        Text("${device.name}  ${device.address}")
                                    }
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("预览", style = MaterialTheme.typography.titleMedium)
                        ProjectionPreview(state, Modifier.fillMaxWidth().aspectRatio(9f / 16f))
                    }
                }
            }

            item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("快捷预设", style = MaterialTheme.typography.titleMedium)
                        projectionPresets.forEach { preset ->
                            ElevatedButton(
                                onClick = { vm.applyPreset(preset) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(preset.name)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("参数", style = MaterialTheme.typography.titleMedium)
                        ColorField("背景颜色", state.bgColor) { color ->
                            vm.updateEditedState { current -> current.copy(bgColor = color) }
                        }
                        SliderField("背景明度", state.bgBrightness.toFloat(), 0f, 100f) { value ->
                            vm.updateEditedState { current -> current.copy(bgBrightness = value.toInt()) }
                        }
                        ColorField("十字颜色", state.crossColor) { color ->
                            vm.updateEditedState { current -> current.copy(crossColor = color) }
                        }
                        SliderField("十字明度", state.crossBrightness.toFloat(), 0f, 100f) { value ->
                            vm.updateEditedState { current -> current.copy(crossBrightness = value.toInt()) }
                        }
                        SliderField("十字大小", state.crossSize, 2f, 16f) { value ->
                            vm.updateEditedState { current -> current.copy(crossSize = value) }
                        }
                        SliderField("线条粗细", state.crossThickness, 0.4f, 5f) { value ->
                            vm.updateEditedState { current -> current.copy(crossThickness = value) }
                        }
                        SliderField("边距比例", state.edgeRatio, 5f, 30f) { value ->
                            vm.updateEditedState { current -> current.copy(edgeRatio = value) }
                        }
                        SliderField("中心高度", state.centerY, 35f, 65f) { value ->
                            vm.updateEditedState { current -> current.copy(centerY = value) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiverScreen(vm: ProjectionViewModel) {
    val state = vm.receiverState
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ProjectionPreview(state, Modifier.fillMaxSize())
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xAA000000))
        ) {
            Text(vm.status, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun ProjectionPreview(state: ProjectionState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        drawProjectionState(state)
    }
}

private fun DrawScope.drawProjectionState(state: ProjectionState) {
    val bg = Color(stateToAndroidColor(state.bgColor))
    val cross = Color(stateToAndroidColor(state.crossColor))
    val width = size.width
    val height = size.height
    val base = minOf(width, height)
    val crossLength = base * (state.crossSize / 100f)
    val thickness = maxOf(1f, base * (state.crossThickness / 100f))
    val inset = base * (state.edgeRatio / 100f)
    val centerY = height * (state.centerY / 100f)

    drawRect(bg)

    fun drawCross(cx: Float, cy: Float) {
        drawRect(
            color = cross,
            topLeft = Offset(cx - crossLength / 2f, cy - thickness / 2f),
            size = Size(crossLength, thickness)
        )
        drawRect(
            color = cross,
            topLeft = Offset(cx - thickness / 2f, cy - crossLength / 2f),
            size = Size(thickness, crossLength)
        )
    }

    drawCross(inset, inset)
    drawCross(width - inset, inset)
    drawCross(width / 2f, centerY)
    drawCross(inset, height - inset)
    drawCross(width - inset, height - inset)
}

@Composable
private fun ColorField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("#00ff00", "#0040d8", "#d8d8d8", "#000000", "#ffffff").forEach { hex ->
                Button(onClick = { onChange(hex) }) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(stateToAndroidColor(hex)))
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("${value.toInt()}")
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}
