package ru.skillbranch.skillarticles.extensions

fun List<Pair<Int, Int>>.groupByBounds(bounds: List<Pair<Int, Int>>): List<List<Pair<Int, Int>>> =
     bounds.map { boundary ->
        this.filter { it.second > boundary.first && it.first < boundary.second }
            .map {
                when {
                    it.first < boundary.first -> boundary.first to it.second
                    it.second > boundary.second -> it.first to boundary.second
                    else -> it
                }
            }
    }
