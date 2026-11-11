package lt.krutas.devboard

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Allows sharing arbitrary files without exposing private file paths.
 * This launches Android's share sheet, NOT a silent upload to the focused app.
 */
class FileShareActivity : Activity() {
    private val pickFile = 401

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivityForResult(pick, pickFile)
            } catch (error: Exception) {
                Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    @Deprecated("System Activity result API is adequate for this small non-Compose activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickFile || resultCode != RESULT_OK) {
            finish()
            return
        }
        val uri = data?.data ?: run { finish(); return }
        val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, "DevBoard attachment", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(send, "Share file using"))
        } catch (error: Exception) {
            Toast.makeText(this, "Unable to share this file", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
