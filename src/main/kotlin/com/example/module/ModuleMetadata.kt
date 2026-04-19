package com.example.module

enum class ModuleCategory(val displayName: String) {
    DISPLAY("Display"),
    ASSISTANT("Assistant"),
    CHEATING("Cheating"),
    OTHER("Other")
}

data class ModuleMetadata(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val dependencies: List<String> = emptyList(),
    val enabled: Boolean = true,
    val category: ModuleCategory = ModuleCategory.OTHER
)
