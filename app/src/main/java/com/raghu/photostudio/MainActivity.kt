package com.raghu.photostudio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.raghu.photostudio.ui.StudioApp
import com.raghu.photostudio.ui.theme.StudioTheme

// AppCompatActivity (not plain ComponentActivity) so the in-app language switch works on every Android version.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                StudioApp()
            }
        }
    }
}
