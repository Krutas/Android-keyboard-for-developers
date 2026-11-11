package lt.krutas.devboard

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast

/** A transient Activity is required to show the Android runtime permission dialog. */
class MicPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone allowed. Return to the keyboard and tap 🎙", Toast.LENGTH_LONG).show()
            finish()
        } else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 42)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42) {
            val allowed = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            Toast.makeText(this, if (allowed) "Microphone enabled. Tap 🎙 again." else "Microphone permission denied", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
