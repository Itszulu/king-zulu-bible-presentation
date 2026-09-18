package com.kingzulu.biblepresentation

/** Logical presentation buses are independent of transport. A Cast, browser or wired output can be assigned to either bus. */
enum class PresentationBus { MAIN, FOLDBACK }
enum class OutputTransport { CAST, BROWSER, WIRED }

object OutputRouting {
    @Volatile private var castBus: PresentationBus = PresentationBus.MAIN
    @Volatile private var browserBus: PresentationBus = PresentationBus.MAIN
    @Volatile private var wiredBus: PresentationBus = PresentationBus.MAIN

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
    }
}
