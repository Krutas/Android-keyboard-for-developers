package lt.krutas.devboard

internal data class ImportConflicts(val profiles: Set<String>, val bindings: Set<String>) {
    val requiresConfirmation get() = profiles.isNotEmpty() || bindings.isNotEmpty()

    companion object {
        fun find(existingProfiles: Set<String>, incomingProfiles: Set<String>,
                 existingBindings: Map<String, String>, incomingBindings: Map<String, String>): ImportConflicts =
            ImportConflicts(existingProfiles.intersect(incomingProfiles), incomingBindings
                .filter { (pkg, profile) -> existingBindings.containsKey(pkg) && existingBindings[pkg] != profile }
                .keys.toSet())
    }
}
