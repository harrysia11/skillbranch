package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(pattern:String, ignoreCase: Boolean = true): List<Int>{

    val listSet:MutableList<Int> = mutableListOf()
    var wholeLength = 0
    var position = 0

    if(this.isNullOrBlank() || pattern.isNullOrBlank()) return listSet

    val list = this.split(" ")
    for(element in list){

        position = element.indexOf(pattern,0,ignoreCase)
        if(position > -1) {
            listSet.add(wholeLength + position)
        }// adding gap between words
        wholeLength += element.length + 1

    }
    return listSet;
}