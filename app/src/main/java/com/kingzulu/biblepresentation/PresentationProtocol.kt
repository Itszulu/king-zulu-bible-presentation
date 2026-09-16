package com.kingzulu.biblepresentation
import org.json.JSONObject
object PresentationProtocol{
 fun slideJson(s:PresentationSlide,t:BackgroundTheme?)=JSONObject().put("type","present").put("version",1).put("reference",s.reference).put("text",s.text).put("translation",s.translation).put("kind",s.kind).toString()
 fun blankJson()=JSONObject().put("type","blank").put("version",1).toString()
}
