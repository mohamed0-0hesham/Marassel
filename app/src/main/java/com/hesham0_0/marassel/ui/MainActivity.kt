package com.hesham0_0.marassel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hesham0_0.marassel.ui.navigation.ChatNavGraph
import com.hesham0_0.marassel.ui.theme.MarasselTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarasselTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatNavGraph()
                }
            }
        }
    }
}