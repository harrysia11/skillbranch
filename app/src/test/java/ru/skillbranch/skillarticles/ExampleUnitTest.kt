package ru.skillbranch.skillarticles

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.skillbranch.skillarticles.data.repositories.Element
import ru.skillbranch.skillarticles.data.repositories.MarkdownParser

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun parse_list_item() {
        val result= MarkdownParser.parse(unorderedListString)
        val actual = prepare<Element.UnorderedListItem>(result.elements)
        assertEquals(expectedUnorderedList, actual)

        printResult(actual)
        println()
        printElements(result.elements)

    }

    @Test
    fun parse_header() {
        val result= MarkdownParser.parse(headerString)
        val actual = prepare<Element.Header>(result.elements)
        assertEquals(expectedHeader, actual)

        printResult(actual)
        println()
        printElements(result.elements)

    }

    @Test
    fun parse_quote() {
        val result= MarkdownParser.parse(quoteString)
        val actual = prepare<Element.Quote>(result.elements)
        assertEquals(expectedQuote, actual)

        printResult(actual)
        println()
        printElements(result.elements)

    }


    @Test
    fun parse_italic() {
        val result= MarkdownParser.parse(italicString)
        val actual = prepare<Element.Italic>(result.elements)
        assertEquals(expectedItalic, actual)

        printResult(actual)
        println()
        printElements(result.elements)

    }
    @Test
    fun parse_strike() {
        val result= MarkdownParser.parse(strikeString)
        val actual = prepare<Element.Strike>(result.elements)
        assertEquals(expectedStrike, actual)

        printResult(actual)
        println()
        printElements(result.elements)

    }

    @Test
    fun parse_combine() {
        val result= MarkdownParser.parse(combineEmphasisString)
        val actualItalic = prepare<Element.Italic>(result.elements)
        val actualBold = prepare<Element.Bold>(result.elements)
        val actualStrike = prepare<Element.Strike>(result.elements)
        assertEquals(expectedCombine["italic"], actualItalic)
        assertEquals(expectedCombine["bold"], actualBold)
        assertEquals(expectedCombine["strike"], actualStrike)

        printResult(actualItalic)
        println()
        printElements(result.elements)
        printResult(actualBold)
        println()
        printElements(result.elements)
        printResult(actualStrike)
        println()
        printElements(result.elements)

    }

    @Test
    fun parse_rule() {
        val result= MarkdownParser.parse(ruleString)
        val actual = prepare<Element.Rule>(result.elements)
        assertEquals(3, actual.size)

        printResult(actual)
        println()
        printElements(result.elements)

    }

    @Test
    fun parse_inline_code() {
        val result= MarkdownParser.parse(inlineString)
        val actual = prepare<Element.InlineCode>(result.elements)
        assertEquals(expectedInline, actual)

        printResult(actual)
        println()
        printElements(result.elements)

    }
    @Test
    fun parse_link() {
        val result= MarkdownParser.parse(linkString)
        val actual = prepare<Element.Link>(result.elements)
        val actualLink = result.elements
            .spread()
            .filterIsInstance<Element.Link>()
            .map { it.link }

        assertEquals(expectedLink["titles"], actual)
        assertEquals(expectedLink["links"], actualLink)

        printResult(actual)
        println()
        printElements(result.elements)

    }

    private fun printResult(list:List<String>){
        val iterator = list.iterator()
        while(iterator.hasNext()){
            println("find -> ${iterator.next()}")
        }
    }

    private fun printElements(list:List<Element>){
        val iterator = list.iterator()
        while(iterator.hasNext()){
            println("element -> ${iterator.next()}")
        }
    }

    private fun Element.spread() : List<Element>{
        val elements = mutableListOf<Element>()
        elements.add(this)
        elements.addAll(this.elements.spread())
        return elements
    }

    private fun List<Element>.spread() : List<Element>{
        val elements = mutableListOf<Element>()
        if(this.isNotEmpty()) elements.addAll(
            this.fold(mutableListOf()){acc, el -> acc.also {
                it.addAll((el.spread()))
                }
            }
        )
        return elements
    }

    private inline fun <reified T : Element> prepare(list: List<Element>) : List<String>{
        return list.fold(mutableListOf<Element>()){ acc, el -> acc.also{it.addAll(el.spread())}}
            .filterIsInstance<T>()
            .map{it.text.toString()}
    }
}
