package com.ilusons.notioff

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ilusons.notioff.ui.main.MainScreen
import com.ilusons.notioff.ui.main.ProfilesViewModel
import com.ilusons.notioff.ui.theme.NotiOffTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ProfilesViewModel by viewModels {
        val app = application as NotiOffApp
        ProfilesViewModel.Factory(
            profileRepository = app.container.profileRepository,
            settingsRepository = app.container.settingsRepository,
            bootstrap = app.container.bootstrap,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiOffTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        onOpenContact = ::openContactEmail,
                        onExitApp = { finishAffinity() },
                    )
                }
            }
        }
    }

    private fun openContactEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${getString(R.string.contact_email)}")
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact_subject))
        }
        runCatching {
            startActivity(Intent.createChooser(intent, getString(R.string.nav_contact)))
        }.onFailure {
            Toast.makeText(this, R.string.contact_no_app, Toast.LENGTH_SHORT).show()
        }
    }
}
