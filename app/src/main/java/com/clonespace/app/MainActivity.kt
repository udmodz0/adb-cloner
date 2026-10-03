package com.clonespace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.clonespace.app.data.shizuku.ShizukuManager
import com.clonespace.app.ui.screens.MainScaffold
import com.clonespace.app.ui.theme.CloneSpaceTheme
import com.clonespace.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CloneSpaceTheme {
                MainScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuManager.refresh(this)
    }
}
