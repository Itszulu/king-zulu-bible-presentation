package com.kingzulu.biblepresentation

data class StageDisplayState(
    val currentText: String = "",
    val nextText: String = "",
    val clockVisible: Boolean = true,
    val privateAlert: String? = null,
    val timerText: String? = null
)

data class OutputRoute(
    val id: String,
    val name: String,
    val kind: String,
    val enabled: Boolean = true
)
