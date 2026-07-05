package com.chroma.projector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private val viewModel: ProjectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            ProjectionApp(vm = viewModel)
        }
    }

    override fun onDestroy() {
        viewModel.shutdown()
        super.onDestroy()
    }
}

