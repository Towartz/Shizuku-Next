package moe.shizuku.manager.hide

import android.os.Bundle
import androidx.activity.compose.setContent
import moe.shizuku.manager.app.AppActivity

class HideAppsActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HideAppsComposeScreen(
                onNavigateUp = { finish() }
            )
        }
    }
}
