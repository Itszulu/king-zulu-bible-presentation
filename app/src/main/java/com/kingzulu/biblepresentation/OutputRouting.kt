package com.kingzulu.biblepresentation

import android.content.Context

/** Logical presentation buses are independent of transport. Cast, browser and wired outputs can be assigned to Main or Foldback. */
enum class PresentationBus { MAIN, FOLDBACK }
enum class OutputTransport { CAST, BROWSER, WIRED }

object OutputRouting {
    @Volatile private var castBus = PresentationBus.MAIN
    @Volatile private var browserBus = PresentationBus.MAIN
    @Volatile private var wiredBus = PresentationBus.MAIN
    @Volatile private var initialized = false
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            val p = appContext!!.getSharedPreferences("king_zulu_output_routing", Context.MODE_PRIVATE)
            castBus = readBus(p.getString("cast_bus", null))
            browserBus = readBus(p.getString("browser_bus", null))
            wiredBus = readBus(p.getString("wired_bus", null))
            initialized = true
        }
    }

    fun busFor(transport: OutputTransport): PresentationBus = when (transport) {
        OutputTransport.CAST -> castBus
        OutputTransport.BROWSER -> browserBus
        OutputTransport.WIRED -> wiredBus
    }

    fun assign(transport: OutputTransport, bus: PresentationBus) {
        when (transport) {
            OutputTransport.CAST -> castBus = bus
            OutputTransport.BROWSER -> browserBus = bus
            OutputTransport.WIRED -> wiredBus = bus
        }
        val key = when (transport) {
            OutputTransport.CAST -> "cast_bus"
            OutputTransport.BROWSER -> "browser_bus"
            OutputTransport.WIRED -> "wired_bus"
        }
        appContext?.getSharedPreferences("king_zulu_output_routing", Context.MODE_PRIVATE)
            ?.edit()?.putString(key, bus.name)?.apply()
    }

    private fun readBus(value: String?): PresentationBus =
        runCatching { PresentationBus.valueOf(value ?: PresentationBus.MAIN.name) }.getOrDefault(PresentationBus.MAIN)
}
