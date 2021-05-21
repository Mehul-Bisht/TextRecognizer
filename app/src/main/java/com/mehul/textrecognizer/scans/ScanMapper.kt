package com.mehul.textrecognizer.scans

data class ScanMapper(
    val id: Int,
    val name: String,
    val recognisedText: String,
    val timeOfStorage: Long,
    val filename: String,
    val isSelected: State
)

enum class State{
    UNINITIALISED,
    INITIALISED,
    SELECTED,
    UNSELECTED,
    UNKNOWN
}