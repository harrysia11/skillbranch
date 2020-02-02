package ru.skillbranch.skillarticles.extensions.data

fun String.indexesOf(pattern:String): List<Int>{

    val listSet:MutableList<Int> = mutableListOf()
    var wholeLength = 0
    var position = 0

    if(pattern.isNullOrBlank()) return listSet

    val list = this.split(" ")
    for(element in list){

        position = element.indexOf(pattern,0,true)
        if(position > -1) {
            listSet.add(wholeLength + position)
        }// adding gap between words
        wholeLength += element.length + 1

    }
    return listSet;
}