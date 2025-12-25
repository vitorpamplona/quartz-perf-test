package com.vitorpamplona.quartzPerfTest

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.quartzPerfTest.ui.theme.SimpleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleTheme {
                val vm: ImporterViewModel =
                    viewModel {
                        ImporterViewModel(
                            app().importer,
                        )
                    }
                ProgressScreen(
                    vm = vm,
                )
            }
        }
    }
}

@Composable
fun app(): App = LocalContext.current.app()

fun Context.app(): App =
    this.applicationContext as App