package com.vitorpamplona.quartzPerfTest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.quartzPerfTest.ui.theme.SimpleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleTheme {
                val viewModel: ProgressViewModel = viewModel()
                ProgressScreen(
                    vm = viewModel,
                )
            }
        }
    }
}