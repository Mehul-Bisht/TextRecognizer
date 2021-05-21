package com.mehul.textrecognizer.scans

fun ScanMapper.mapToScan() =
    Scan(
        id = id,
        name = name,
        recognisedText = recognisedText,
        timeOfStorage = timeOfStorage,
        filename = filename
    )

fun Scan.toScanMapper(isSelected: State) =
    ScanMapper(
        id = id,
        name = name,
        recognisedText = recognisedText,
        timeOfStorage = timeOfStorage,
        filename = filename,
        isSelected = isSelected
    )