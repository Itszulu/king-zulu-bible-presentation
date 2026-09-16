package com.kingzulu.biblepresentation

data class BibleReference(val book:String,val chapter:Int,val verseStart:Int?=null,val verseEnd:Int?=null){
    fun display()=buildString{append("$book $chapter");verseStart?.let{append(":$it");if(verseEnd!=null&&verseEnd!=it)append("–$verseEnd")}}
}

object BibleReferenceParser{
    private val aliases=mapOf(
        "jn" to "John","jhn" to "John","john" to "John","mt" to "Matthew","matt" to "Matthew","matthew" to "Matthew",
        "mk" to "Mark","mark" to "Mark","lk" to "Luke","luke" to "Luke","rom" to "Romans","ro" to "Romans","romans" to "Romans",
        "ps" to "Psalms","psalm" to "Psalms","psalms" to "Psalms","gen" to "Genesis","genesis" to "Genesis",
        "rev" to "Revelation","revelation" to "Revelation","acts" to "Acts","act" to "Acts","isa" to "Isaiah","isaiah" to "Isaiah"
    )
    fun parse(input:String):BibleReference?{
        val cleaned=input.lowercase().trim().replace(Regex("([a-z])(\\d)"),"$1 $2").replace(":"," ").replace(Regex("\\s+")," ")
        val p=cleaned.split(" "); if(p.size<2)return null
        val book=aliases[p[0]]?:p[0].replaceFirstChar{it.uppercase()}
        val ch=p.getOrNull(1)?.toIntOrNull()?:return null
        val range=p.getOrNull(2)?.split("-","–")
        return BibleReference(book,ch,range?.getOrNull(0)?.toIntOrNull(),range?.getOrNull(1)?.toIntOrNull())
    }
}
