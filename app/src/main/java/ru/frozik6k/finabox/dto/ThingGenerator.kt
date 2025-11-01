package ru.frozik6k.finabox.dto



object ThingGenerator {
    private val letterList: List<String> = listOf(
        "K",
        "П",
        "Q",
        "L",
        "E"
    )

    fun generateThings(count: Int): List<ThingDto> =
        (0..count).map { index ->
            ThingDto(
                letter = letterList.random(),
                name = "Событие $index"
            )
        }
}