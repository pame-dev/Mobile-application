package com.pame.karsy

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pame.karsy.core.locale.Idioma
import com.pame.karsy.core.navigation.KarsyNavGraph
import com.pame.karsy.core.theme.KarsyTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Idioma.envolver(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KarsyTheme {
                KarsyNavGraph()
            }
        }
    }
}