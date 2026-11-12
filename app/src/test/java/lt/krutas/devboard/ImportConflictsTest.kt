package lt.krutas.devboard

import org.junit.Assert.*
import org.junit.Test

class ImportConflictsTest {
    @Test fun detectsExistingProfilesAndChangedBindings() {
        val conflicts = ImportConflicts.find(setOf("Default", "Work"), setOf("Work", "New"),
            mapOf("com.a" to "Default", "com.b" to "Work"),
            mapOf("com.a" to "New", "com.b" to "Work", "com.c" to "New"))
        assertEquals(setOf("Work"), conflicts.profiles)
        assertEquals(setOf("com.a"), conflicts.bindings)
        assertTrue(conflicts.requiresConfirmation)
    }
    @Test fun newProfilesAndUnchangedOrNewBindingsNeedNoConfirmation() {
        val conflicts = ImportConflicts.find(setOf("Default"), setOf("New"),
            mapOf("com.a" to "Default"), mapOf("com.a" to "Default", "com.b" to "New"))
        assertFalse(conflicts.requiresConfirmation)
    }
}
