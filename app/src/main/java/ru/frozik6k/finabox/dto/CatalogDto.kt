package ru.frozik6k.finabox.dto

enum class CatalogType {
    THING,
    BOX
}

data class CatalogDto(
    val id: Long,
    val letter: String,
    val name: String,
    val type: CatalogType,
    val parentBox: String?,
)