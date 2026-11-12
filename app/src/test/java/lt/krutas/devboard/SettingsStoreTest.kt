package lt.krutas.devboard

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsStoreTest {
    @Test fun fourVariantEditsSurviveJsonRoundTripAndProfileClone() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = SettingsStore.prefs(context)
        val expected = linkedMapOf<Pair<String, String>, List<List<String>>>()
        for (language in listOf("lt-LT", "en-US")) for (variant in listOf("pc", "qwerty")) {
            prefs.edit().putString("language", language).putString("variant_$language", variant).apply()
            val rows = SettingsStore.rows(context)
            val first = rows[0][0]; rows[0][0] = rows[0][3]; rows[0][3] = first
            rows[1].removeAt(0)
            SettingsStore.saveRows(context, rows)
            expected[language to variant] = rows.map { it.toList() }
        }
        val exported = SettingsStore.exportProfiles(context)
        prefs.edit().clear().commit()
        assertEquals(1, SettingsStore.applyProfileImport(context, SettingsStore.prepareProfileImport(context, exported)))
        for ((selection, rows) in expected) {
            val (language, variant) = selection
            prefs.edit().putString("language", language).putString("variant_$language", variant).apply()
            assertEquals("Imported $selection", rows, SettingsStore.rows(context))
        }
        SettingsStore.addProfile(context, "Clone")
        SettingsStore.resetSelectedRows(context)
        assertNotEquals(expected["en-US" to "qwerty"], SettingsStore.rows(context))
        SettingsStore.setActiveProfile(context, "Default")
        assertEquals(expected["en-US" to "qwerty"], SettingsStore.rows(context))
        SettingsStore.deleteProfile(context, "Clone")
        assertEquals(setOf("Default"), SettingsStore.profiles(context).keys)
        // Changing language preserves its independently selected variant.
        prefs.edit().putString("language", "lt-LT").apply()
        assertEquals("qwerty", SettingsStore.layoutVariant(context))
        prefs.edit().putString("variant_lt-LT", "pc").putString("language", "en-US").apply()
        assertEquals("qwerty", SettingsStore.layoutVariant(context))
    }

    @Test fun layoutEditorSwapsDeletesAndConfirmsReset() {
        val controller = Robolectric.buildActivity(LayoutEditorActivity::class.java).setup()
        val activity = controller.get()
        fun descendants(view: View): List<View> = listOf(view) +
            if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
        fun button(label: String) = descendants(activity.window.decorView).filterIsInstance<Button>().first { it.text.toString() == label }
        button("Esc").performClick(); button("Tab").performClick()
        assertEquals(listOf("Tab", "Ctrl", "Alt", "Esc"), SettingsStore.rows(activity)[0].take(4))
        button("Tab").performLongClick()
        shadowOf(ShadowAlertDialog.getLatestAlertDialog()).clickOnItem(0)
        assertEquals(7, SettingsStore.rows(activity)[0].size)
        val edited = SettingsStore.rows(activity)
        button("Reset to defaults").performClick()
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(edited, SettingsStore.rows(activity))
        button("Reset to defaults").performClick()
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(SettingsStore.defaultRows, SettingsStore.rows(activity))
        controller.pause().stop().destroy()
    }
}
