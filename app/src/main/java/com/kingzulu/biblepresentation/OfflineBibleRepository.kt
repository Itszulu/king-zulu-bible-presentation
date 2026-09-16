package com.kingzulu.biblepresentation

data class Verse(val reference:BibleReference,val text:String,val translation:String)
object OfflineBibleRepository{
    private val kjv=mapOf(
        "John 3:16" to "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
        "John 5:24" to "Verily, verily, I say unto you, He that heareth my word, and believeth on him that sent me, hath everlasting life, and shall not come into condemnation; but is passed from death unto life.",
        "Romans 8:28" to "And we know that all things work together for good to them that love God, to them who are the called according to his purpose.",
        "Matthew 6:33" to "But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you.",
        "Psalms 23:1" to "The LORD is my shepherd; I shall not want."
    )
    fun get(r:BibleReference,translation:String="KJV"):Verse?{if(translation!="KJV"||r.verseStart==null)return null;return kjv[r.display()]?.let{Verse(r,it,translation)}}
}
