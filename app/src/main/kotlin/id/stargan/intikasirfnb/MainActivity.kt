package id.stargan.intikasirfnb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import id.stargan.intikasirfnb.debug.DebugSeeder
import id.stargan.intikasirfnb.navigation.PosNavGraph
import id.stargan.intikasirfnb.ui.theme.PosTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var debugSeeder: DebugSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PosTheme {
                PosNavGraph(debugSeeder = if (BuildConfig.DEBUG) debugSeeder else null)
            }
        }
    }
}
