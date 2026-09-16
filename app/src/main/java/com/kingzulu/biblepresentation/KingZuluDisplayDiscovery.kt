package com.kingzulu.biblepresentation

import android.content.Context

data class DiscoveredDisplay(val name:String,val host:String,val port:Int,val protocol:String="King Zulu")
class KingZuluDisplayDiscovery(private val context:Context){
    fun start(onFound:(DiscoveredDisplay)->Unit){}
    fun stop(){}
}
