package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.navigation.TwinGuardNavGraph
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.TwinGuardTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TwinGuardTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkNavyBackground)
                ) {
                    val navController = rememberNavController()
                    TwinGuardNavGraph(navController = navController)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // System will automatically resize Compose layout in PiP window
    }
}
