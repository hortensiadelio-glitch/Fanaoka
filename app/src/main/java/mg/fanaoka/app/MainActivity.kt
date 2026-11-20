package mg.fanaoka.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mg.fanaoka.app.protection.FanaokaVpnService

class MainActivity : ComponentActivity() {
    private val requestCode = 4101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var active by remember { mutableStateOf(FanaokaVpnService.isRunning) }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("FANAOKA", style = MaterialTheme.typography.headlineLarge)
                        Text("Stop pub • Économisez vos données mobiles")
                        Text(if (active) "Protection activée" else "Protection désactivée",
                            style = MaterialTheme.typography.titleMedium)
                        Button(onClick = {
                            if (active) {
                                stopService(Intent(this@MainActivity, FanaokaVpnService::class.java))
                                active = false
                            } else {
                                val consent = VpnService.prepare(this@MainActivity)
                                if (consent == null) {
                                    startProtection()
                                    active = true
                                } else {
                                    startActivityForResult(consent, requestCode)
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (active) "Désactiver la protection" else "Activer la protection")
                        }
                        Text("Alpha 0.1.1 • Filtrage DNS local",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Le filtrage DNS ne bloque pas toutes les publicités. Les résultats doivent être testés.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    private fun startProtection() {
        val intent = Intent(this, FanaokaVpnService::class.java)
        startService(intent)
    }

    @Deprecated("Compatibilité avec le dialogue de consentement VPN")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) startProtection()
    }
}
