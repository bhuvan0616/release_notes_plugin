package dev.bhuvan.dialog

data class ReleaseNotesModel(
    val features: List<String> = emptyList(),
    val bugs: List<String> = emptyList(),
    val authorName: String,
    val authorEmail: String
)