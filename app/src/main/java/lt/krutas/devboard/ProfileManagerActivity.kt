package lt.krutas.devboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Android profiles also accept exported DevBoard web simulator JSON. */
class ProfileManagerActivity : Activity() {
    private val importRequest = 501
    private val exportRequest = 502
    private lateinit var root: LinearLayout
    private fun dp(n: Int) = (resources.displayMetrics.density * n).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroller = ScrollView(this).apply { setBackgroundColor(Color.rgb(15, 23, 38)) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(22), dp(16), dp(22))
        }
        scroller.addView(root)
        setContentView(scroller)
        render()
    }

    private fun title(text: String) {
        root.addView(TextView(this).apply {
            this.text = text
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, dp(12), 0, dp(6))
        })
    }

    private fun note(text: String) {
        root.addView(TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.rgb(174, 192, 217))
            setPadding(0, dp(3), 0, dp(9))
        })
    }

    private fun action(text: String, onClick: () -> Unit) {
        root.addView(Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        })
    }

    private fun render() {
        root.removeAllViews()
        title("KEYBOARD PROFILES")
        note("Each profile stores a 7-row Terminal layout. Tap a name to make it active. Changes in Layout Editor are saved to the active profile.")
        val selected = SettingsStore.activeProfile(this)
        SettingsStore.profiles(this).keys.forEach { name ->
            val line = LinearLayout(this)
            line.addView(Button(this).apply {
                text = (if (name == selected) "✓ " else "") + name
                isAllCaps = false
                setOnClickListener { SettingsStore.setActiveProfile(this@ProfileManagerActivity, name); render() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (SettingsStore.profiles(this).size > 1) {
                line.addView(Button(this).apply {
                    text = "✕"
                    setOnClickListener {
                        AlertDialog.Builder(this@ProfileManagerActivity)
                            .setTitle("Delete profile?")
                            .setMessage("Delete \"" + name + "\" and its layout?")
                            .setPositiveButton("Delete") { _, _ -> SettingsStore.deleteProfile(this@ProfileManagerActivity, name); render() }
                            .setNegativeButton("Cancel", null).show()
                    }
                })
            }
            root.addView(line)
        }
        val name = EditText(this).apply { hint = "New profile name"; setSingleLine(true) }
        root.addView(name)
        action("+ Clone active layout as new profile") {
            try {
                SettingsStore.addProfile(this, name.text.toString().trim())
                render()
            } catch (error: Exception) { toast(error.message ?: "Cannot create profile") }
        }
        title("Import / Export")
        note("Import web simulator JSON or a previously exported Android layout. Only Terminal rows are used. Existing profiles with identical names are replaced.")
        // Side-by-side actions with equal width; never stack Import and Export.
        val importExportRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        importExportRow.addView(Button(this).apply {
            text = "↓ Import"
            isAllCaps = false
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }, importRequest)
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(4)
        })
        importExportRow.addView(Button(this).apply {
            text = "↑ Export"
            isAllCaps = false
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/json"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_TITLE, "devboard-layouts.json")
                }, exportRequest)
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(4)
        })
        root.addView(importExportRow)
        title("App-specific layouts")
        note("Enable auto-selection to use a different profile in an app. It matches the package name reported by Android for the active text field. No accessibility permission or app usage tracking.")
        root.addView(CheckBox(this).apply {
            text = "Automatically choose profile per app"
            setTextColor(Color.WHITE)
            isChecked = SettingsStore.autoProfile(this@ProfileManagerActivity)
            setOnCheckedChangeListener { _, checked ->
                SettingsStore.prefs(this@ProfileManagerActivity).edit().putBoolean("auto_profile", checked).apply()
            }
        })
        val pkg = EditText(this).apply { hint = "App package, e.g. com.termux"; setSingleLine(true) }
        root.addView(pkg)
        val assigned = SettingsStore.activeProfile(this)
        note("Choose the target profile by selecting it above, then add an app package here.")
        action("+ Assign app to \"" + assigned + "\"") {
            val packageName = pkg.text.toString().trim()
            if (!packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
                toast("Enter a valid Android package name")
                return@action
            }
            val bindings = SettingsStore.appBindings(this)
            bindings[packageName] = SettingsStore.activeProfile(this)
            SettingsStore.setAppBindings(this, bindings)
            render()
        }
        SettingsStore.appBindings(this).forEach { (packageName, profile) ->
            val line = LinearLayout(this)
            line.addView(TextView(this).apply {
                text = packageName + " → " + profile
                setTextColor(Color.WHITE)
                textSize = 12f
                setPadding(dp(6), dp(12), dp(6), dp(12))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            line.addView(Button(this).apply {
                text = "✕"
                setOnClickListener {
                    val bindings = SettingsStore.appBindings(this@ProfileManagerActivity)
                    bindings.remove(packageName)
                    SettingsStore.setAppBindings(this@ProfileManagerActivity, bindings)
                    render()
                }
            })
            root.addView(line)
        }
        action("Done") { finish() }
    }

    @Deprecated("Uses the platform document picker; no storage permissions required")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try {
            when (requestCode) {
                importRequest -> {
                    val file = contentResolver.openInputStream(uri) ?: error("Cannot open file")
                    val text = file.bufferedReader().use { reader ->
                        // Bound input size before parsing untrusted JSON.
                        val buffer = CharArray(256_001)
                        val count = reader.read(buffer)
                        String(buffer, 0, count.coerceAtLeast(0))
                    }
                    toast("Imported " + SettingsStore.importProfiles(this, text) + " profile(s)")
                }
                exportRequest -> {
                    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(SettingsStore.exportProfiles(this))
                    } ?: error("Cannot write file")
                    toast("Profiles exported")
                }
            }
        } catch (error: Exception) {
            toast(error.message ?: "File error")
        }
        render()
    }

    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
