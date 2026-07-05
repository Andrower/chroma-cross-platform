package com.chroma.projector

import android.graphics.Color as AndroidColor
import org.json.JSONObject

enum class LaunchMode {
    Choose,
    Control,
    Receiver
}

data class ProjectionState(
    val bgColor: String = "#00ff00",
    val bgBrightness: Int = 100,
    val crossColor: String = "#0040d8",
    val crossBrightness: Int = 100,
    val crossSize: Float = 6f,
    val crossThickness: Float = 1.4f,
    val edgeRatio: Float = 10f,
    val centerY: Float = 50f
)

data class ProjectionPreset(
    val name: String,
    val bgColor: String,
    val bgBrightness: Int,
    val crossColor: String,
    val crossBrightness: Int
)

data class ManagedDevice(
    val id: String,
    val name: String,
    val address: String,
    val lastSeen: Long,
    val connected: Boolean,
    val state: ProjectionState
)

data class DiscoveredControl(
    val host: String,
    val port: Int,
    val name: String,
    val lastSeen: Long
)

val defaultProjectionState = ProjectionState()

val projectionPresets = listOf(
    ProjectionPreset("绿底蓝十字", "#00ff00", 100, "#0040d8", 100),
    ProjectionPreset("60%绿底蓝十字", "#00ff00", 60, "#0040d8", 100),
    ProjectionPreset("30%绿底蓝十字", "#00ff00", 30, "#0040d8", 100),
    ProjectionPreset("蓝底绿十字", "#0040d8", 100, "#00ff00", 100),
    ProjectionPreset("60%蓝底绿十字", "#0040d8", 60, "#00ff00", 100),
    ProjectionPreset("30%蓝底绿十字", "#0040d8", 30, "#00ff00", 100),
    ProjectionPreset("浅灰底蓝十字", "#d8d8d8", 100, "#0040d8", 100),
    ProjectionPreset("浅灰底绿十字", "#d8d8d8", 100, "#00ff00", 100)
)

fun ProjectionState.toJson(): JSONObject = JSONObject().apply {
    put("bgColor", bgColor)
    put("bgBrightness", bgBrightness)
    put("crossColor", crossColor)
    put("crossBrightness", crossBrightness)
    put("crossSize", crossSize)
    put("crossThickness", crossThickness)
    put("edgeRatio", edgeRatio)
    put("centerY", centerY)
}

fun stateFromJson(json: JSONObject): ProjectionState = ProjectionState(
    bgColor = json.optString("bgColor", defaultProjectionState.bgColor),
    bgBrightness = json.optInt("bgBrightness", defaultProjectionState.bgBrightness),
    crossColor = json.optString("crossColor", defaultProjectionState.crossColor),
    crossBrightness = json.optInt("crossBrightness", defaultProjectionState.crossBrightness),
    crossSize = json.optDouble("crossSize", defaultProjectionState.crossSize.toDouble()).toFloat(),
    crossThickness = json.optDouble("crossThickness", defaultProjectionState.crossThickness.toDouble()).toFloat(),
    edgeRatio = json.optDouble("edgeRatio", defaultProjectionState.edgeRatio.toDouble()).toFloat(),
    centerY = json.optDouble("centerY", defaultProjectionState.centerY.toDouble()).toFloat()
)

fun stateToAndroidColor(hex: String): Int = AndroidColor.parseColor(hex)

