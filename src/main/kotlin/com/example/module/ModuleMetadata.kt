package com.example.module

data class ModuleMetadata(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val dependencies: List<String> = emptyList(),
    val enabled: Boolean = true
)
