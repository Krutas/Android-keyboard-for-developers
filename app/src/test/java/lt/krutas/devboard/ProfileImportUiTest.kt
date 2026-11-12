package lt.krutas.devboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Looper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileImportUiTest {
    private fun import(activity: ProfileManagerActivity, json: JSONObject) {
        val uri = Uri.parse("content://devboard.test/profile.json")
        shadowOf(activity.contentResolver).registerInputStream(uri, json.toString().byteInputStream())
        ReflectionHelpers.callInstanceMethod<Void>(activity, "onActivityResult",
            ClassParameter.from(Int::class.javaPrimitiveType, 501),
            ClassParameter.from(Int::class.javaPrimitiveType, Activity.RESULT_OK),
            ClassParameter.from(Intent::class.java, Intent().setData(uri)))
    }

    @Test fun conflictingImportChangesNothingUntilConfirmed() {
        val controller = Robolectric.buildActivity(ProfileManagerActivity::class.java).setup()
        val activity = controller.get()
        SettingsStore.prefs(activity).edit().clear().commit()
        SettingsStore.setAppBindings(activity, mapOf("com.example" to "Default"))
        val before = SettingsStore.exportProfiles(activity)
        val incoming = JSONObject(before).apply {
            put("autoProfile", true)
            getJSONObject("profiles").put("Work", getJSONObject("profiles").getJSONObject("Default"))
            getJSONObject("appBindings").put("com.example", "Work")
        }
        import(activity, incoming)
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(dialog)
        assertEquals(before, SettingsStore.exportProfiles(activity))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(before, SettingsStore.exportProfiles(activity))
        import(activity, incoming)
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(SettingsStore.profiles(activity).containsKey("Work"))
        assertEquals("Work", SettingsStore.appBindings(activity)["com.example"])
        assertTrue(SettingsStore.autoProfile(activity))
        controller.pause().stop().destroy()
    }

    @Test fun legacyWebImportRestoresBindingsAndAutoWithoutConflict() {
        val controller = Robolectric.buildActivity(ProfileManagerActivity::class.java).setup()
        val activity = controller.get()
        SettingsStore.prefs(activity).edit().clear().commit()
        val exported = JSONObject(SettingsStore.exportProfiles(activity))
        val profile = exported.getJSONObject("profiles").getJSONObject("Default")
        val incoming = JSONObject().put("profiles", JSONObject().put("Web", profile))
            .put("profilePanel", JSONObject().put("auto", true)
                .put("bindings", JSONObject().put("com.web", "Web")))
        import(activity, incoming)
        assertTrue(SettingsStore.profiles(activity).containsKey("Web"))
        assertEquals("Web", SettingsStore.appBindings(activity)["com.web"])
        assertTrue(SettingsStore.autoProfile(activity))
        controller.pause().stop().destroy()
    }
}
