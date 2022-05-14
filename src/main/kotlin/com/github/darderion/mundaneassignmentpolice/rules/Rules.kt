package com.github.darderion.mundaneassignmentpolice.rules

import com.github.darderion.mundaneassignmentpolice.checker.PunctuationMark
import com.github.darderion.mundaneassignmentpolice.checker.RuleViolationType
import com.github.darderion.mundaneassignmentpolice.checker.isPunctuationMark
import com.github.darderion.mundaneassignmentpolice.checker.rule.formula.FormulaPunctuationRuleBuilder
import com.github.darderion.mundaneassignmentpolice.checker.rule.list.ListRuleBuilder
import com.github.darderion.mundaneassignmentpolice.checker.rule.regex.RegexRuleBuilder
import com.github.darderion.mundaneassignmentpolice.checker.rule.symbol.SymbolRuleBuilder
import com.github.darderion.mundaneassignmentpolice.checker.rule.symbol.and
import com.github.darderion.mundaneassignmentpolice.checker.rule.symbol.or
import com.github.darderion.mundaneassignmentpolice.checker.rule.tableofcontent.TableOfContentRuleBuilder
import com.github.darderion.mundaneassignmentpolice.checker.rule.url.URLRuleBuilder
import com.github.darderion.mundaneassignmentpolice.checker.rule.word.WordRule
import com.github.darderion.mundaneassignmentpolice.checker.rule.word.WordRuleBuilder
import com.github.darderion.mundaneassignmentpolice.checker.rule.word.or
import com.github.darderion.mundaneassignmentpolice.pdfdocument.PDFArea
import com.github.darderion.mundaneassignmentpolice.pdfdocument.PDFRegion
import com.github.darderion.mundaneassignmentpolice.utils.URLUtil
import java.util.*

private val enLetters = "abcdefghijklmnopqrstuvwxyz"
private val enCapitalLetters = enLetters.uppercase(Locale.getDefault())
private val EN = enLetters + enCapitalLetters

private val rusLetters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
private val rusCapitalLetters = rusLetters.uppercase(Locale.getDefault())
private val RU = rusLetters + rusCapitalLetters

private val numbers = "0123456789"

val RULE_LITLINK = SymbolRuleBuilder()
	.symbol('?')
	.ignoringAdjusting(*" ,$numbers".toCharArray())
	.shouldNotHaveNeighbor(*"[]".toCharArray())
	//.called("Symbol '?' in litlink")
	.called("Символ ? в ссылке на литературу")
	.getRule()

val shortDash = '-'

// 3-way road
// one-sided battle

val shortDashRules = SymbolRuleBuilder()
	.symbol(shortDash)
	.shouldHaveNeighbor(*EN.toCharArray())
	.shouldHaveNeighbor(*RU.toCharArray())
	.shouldHaveNeighbor(*numbers.toCharArray())
	//.called("Incorrect usage of '-' symbol")
	.called("Неправильное использование дефиса")
	.inArea(PDFRegion.EVERYWHERE.except(PDFArea.BIBLIOGRAPHY, PDFArea.FOOTNOTE))

val RULE_SHORT_DASH = shortDashRules.getRule() and (
		shortDashRules.fromLeft().shouldHaveNeighbor('.')
			.shouldNotHaveNeighbor(*numbers.toCharArray()).getRule() or
				shortDashRules.fromRight().shouldHaveNeighbor('\n')
					.shouldNotHaveNeighbor(*numbers.toCharArray()).getRule()
		)

val mediumDash = '–'

val RULE_MEDIUM_DASH = SymbolRuleBuilder()
	.symbol(mediumDash)
	.shouldHaveNeighbor(*numbers.toCharArray())
	//.called("Incorrect usage of '--' symbol")
	.called("Неправильное использование короткого тире")
	.inArea(PDFRegion.EVERYWHERE.except(PDFArea.BIBLIOGRAPHY, PDFArea.FOOTNOTE))
	.ignoringIfIndex(0)
	.getRule()

val longDash = '—'

val RULE_LONG_DASH = SymbolRuleBuilder()
	.symbol(longDash)
	.ignoringAdjusting(' ')
	.shouldNotHaveNeighbor(*numbers.toCharArray())
	//.called("Incorrect usage of '---' symbol")
	.called("Неправильное использование длинного тире")
	.inArea(PDFRegion.EVERYWHERE.except(PDFArea.BIBLIOGRAPHY, PDFArea.FOOTNOTE))
	.getRule() and SymbolRuleBuilder()
	.symbol(longDash)
	.shouldHaveNeighbor(' ')
	.inArea(PDFRegion.EVERYWHERE.except(PDFArea.BIBLIOGRAPHY, PDFArea.FOOTNOTE))
	.getRule()

val closingQuote = '”'
val openingQuote = '“'

val RULE_CLOSING_QUOTATION = SymbolRuleBuilder()
	.symbol(closingQuote)
	.ignoringEveryCharacterExcept(*"$closingQuote$openingQuote".toCharArray())
	.fromLeft().shouldHaveNeighbor(openingQuote)
	.inNeighborhood(20)
	.called("Неправильное использование закрывающей кавычки")
	.getRule()

val RULE_OPENING_QUOTATION = SymbolRuleBuilder()
	.symbol(openingQuote)
	.ignoringEveryCharacterExcept(*"$closingQuote$openingQuote".toCharArray())
	.fromRight().shouldHaveNeighbor(closingQuote)
	.inNeighborhood(20)
	.called("Неправильное использование открывающей кавычки")
	.getRule()

const val squareClosingBracket = ']'
const val squareOpeningBracket = '['

val RULE_MULTIPLE_LITLINKS = SymbolRuleBuilder()
	.symbol(squareClosingBracket)
	.ignoringAdjusting(' ', ',')
	.fromRight().shouldNotHaveNeighbor(squareOpeningBracket)
	.called("Неправильное оформление нескольких ссылок")
	.getRule()

const val bracket = '('

val RULE_BRACKETS_LETTERS = SymbolRuleBuilder()
	.symbol(bracket)
	.ignoringAdjusting(' ')
	.fromRight().shouldNotHaveNeighbor(*rusCapitalLetters.toCharArray())
	.called("Большая русская буква после скобки")
	.type(RuleViolationType.Warning)
	.getRule()

private const val openingBrackets = "([{<"
private const val closingBrackets = ")]}>"
private const val closingQuotes = "”»"
private const val punctuationSymbols = ".,;:!?"

private val spaceAroundBracketsRuleBuilders = List(2) { SymbolRuleBuilder() }
	.map { it.shouldHaveNeighbor(' ', '\n') }
	.map { it.called("Отсутствует пробел с внешней стороны скобок") }
	.apply {
		// setting up a rule that should look for a space before opening brackets
		first().fromLeft().ignoringAdjusting(*openingBrackets.toCharArray())
		// and this rule should look for after closing brackets
		last().fromRight()
			.ignoringAdjusting(*"$punctuationSymbols$closingQuotes$closingBrackets".toCharArray())
	}

// For case when round brackets are empty: "function()"
private val openingRoundBracketExceptionalRule = SymbolRuleBuilder()
	.symbol('(')
	.fromRight().shouldHaveNeighbor(')')
	.getRule()

val RULES_SPACE_AROUND_BRACKETS = spaceAroundBracketsRuleBuilders
	.zip(listOf(openingBrackets, closingBrackets).map { it.toCharArray() })
	.map { pair -> pair.second.map { pair.first.symbol(it).getRule() } }
	.flatten()
	.map {
		if (it.symbol == '(') it or openingRoundBracketExceptionalRule
		else it
	}

val RULE_CITATION = SymbolRuleBuilder()
	.symbol('[')
	.ignoringAdjusting(' ', '\n')
	.fromLeft().shouldNotHaveNeighbor('.')
	.called("Некорректное цитирование")
	.inArea(PDFArea.SECTION)
	.getRule()

val RULE_SINGLE_SUBSECTION = ListRuleBuilder()
	.inArea(PDFRegion.NOWHERE.except(PDFArea.TABLE_OF_CONTENT))
	//.called("Only 1 subsection in a section")
	.called("Одна подсекция в секции")
	.disallow {
		if (it.nodes.count() == 1) it.nodes.first().getText() else listOf()
	}.getRule()

val RULE_TABLE_OF_CONTENT_NUMBERS = TableOfContentRuleBuilder()
	.disallow {
		it.filter {
			// println("${it.text.count()} -> ${it.content}")
			val text = it.text.filter { it.text.trim().isNotEmpty() }
			((text.count() == 3 && (text[1].text == "Введение" || text[1].text == "Заключение")) ||
					(text.count() == 4 && text[1].text == "Список" && text[2].text == "литературы"))
		}
	}.called("Введение, заключение и список литературы не нумеруются")
	.getRule()

val RULE_SYMBOLS_IN_SECTION_NAMES = TableOfContentRuleBuilder()
	.disallow { listOfLines ->
		listOfLines.filter { line ->
			val text = line.text.filterNot { it.text == "." }           // remove leaders
				.filterNot { it.text.contains("[0-9]+\\.".toRegex()) }  // remove numbering
				.joinToString("")
			text.contains("[:.,]".toRegex())
		}
	}.called("""Символы ":", ".", "," в названии секции""")
	.getRule()

val smallNumbersRuleName = "Неправильное написание целых чисел от 1 до 9"
val smallNumbersRuleArea =
	PDFRegion.EVERYWHERE.except(PDFArea.PAGE_INDEX, PDFArea.TABLE_OF_CONTENT, PDFArea.BIBLIOGRAPHY)
val allowedWordsOnLeft = arrayOf(
	Regex("""[Рр]ис[a-я]*"""),
	Regex("""[Тт]абл[a-я]*"""), Regex("""[Сс]х[a-я]*"""),
	Regex("""[Dd]ef[a-z]*"""), Regex("""[Оо]пр[а-я]*"""),
	Regex("""[Tt]h[a-z]*"""), Regex("""[Тт]еорема""")
)
val allowedWordsOnRight = arrayOf(
	Regex("""[Gg][Bb]"""), Regex("""[Гг][Бб]"""),
	Regex("""[Mm][Bb]"""), Regex("""[Мм][Бб]"""),
	Regex("""[Gg][Hh][Zz]"""), Regex("""[Гг][Цц]"""),
	Regex("""→""")
)

val smallNumbersRuleBuilder1 = WordRuleBuilder()		//for nearest words
	.called(smallNumbersRuleName)
	.inArea(smallNumbersRuleArea)
	.ignoringAdjusting(Regex("""\s"""), Regex("""\."""))
	.ignoringIfIndex(0)

val smallNumbersRuleBuilder2 = WordRuleBuilder()		//for decimal fractions and version numbers
	.called(smallNumbersRuleName)
	.inArea(smallNumbersRuleArea)
	.shouldHaveNeighbor(Regex("""\."""), Regex(""","""),
		Regex("""[0-9]+"""))
	.shouldHaveNumberOfNeighbors(2)

val smallNumbersRuleBuilder3 = WordRuleBuilder()		//for links
	.called(smallNumbersRuleName)
	.inArea(smallNumbersRuleArea)
	.fromLeft()
	.ignoringWords(true)
	.ignoringAdjusting(Regex(""","""), Regex("""\s"""))
	.shouldHaveNeighbor(Regex("""\["""))

val RULES_SMALL_NUMBERS = List<WordRule>(9) { index ->
	smallNumbersRuleBuilder1.word((index + 1).toString())
		.fromLeft().shouldHaveNeighbor(*allowedWordsOnLeft).getRule() or
	smallNumbersRuleBuilder1.word((index + 1).toString())
		.fromRight().shouldHaveNeighbor(*allowedWordsOnRight).getRule() or
	smallNumbersRuleBuilder2.word((index + 1).toString()).fromLeft().getRule() or
	smallNumbersRuleBuilder2.fromRight().getRule() or
	smallNumbersRuleBuilder3.word((index + 1).toString()).getRule()
}

val RULE_SHORTENED_URLS = URLRuleBuilder()
	.called("Сокращённая ссылка")
	.disallow { urls ->
		urls.filter { pair ->
			try {
				var url = pair.first
				if (!url.startsWith("http")) url = "https://$url"
				URLUtil.isShortened(url)
			} catch (_: Exception) {
				false
			}
		}.map { it.second }
	}.getRule()

val RULE_ORDER_OF_REFERENCES = RegexRuleBuilder()
	.called("Неверный порядок ссылок на литературу")
	.regex(Regex("""\[[0-9,\s]+\]"""))
	.searchIn(1)
	.disallow { matches ->
		matches.filter { pair ->
			val references = pair.first
			val referencesInIntList = references
				.slice(IntRange(1, references.length - 2))
				.split(Regex(""","""))
				.map { it.trim().toInt() }
			referencesInIntList != referencesInIntList.sorted()
		}.map { it.second }
	}.getRule()

private val ignoringAfterFormula = listOf(
	"""\s""".toRegex(),
	"""\([0-9]+\)""".toRegex()  // ignore formula reference, e.g. "(1)"
)

val fullStopAfterFormulaRule = FormulaPunctuationRuleBuilder()
	.called("Отсутствует точка после формулы")
	.ignoredWords(*ignoringAfterFormula.toTypedArray())
	.rule { formula, filteredText, nextFormula ->
		val violationLines = listOf(formula.lines.last())
		val lastFormulaSymbol = formula.text.last().text.last()

		if (filteredText.isEmpty()) {
			return@rule if (lastFormulaSymbol != PunctuationMark.FULL_STOP.value) violationLines else emptyList()
		}

		val (firstAfterFormula, secondAfterFormula) = filteredText.first() to filteredText.getOrNull(1)
		if (nextFormula != null &&
			(firstAfterFormula == nextFormula.text.first() ||
				firstAfterFormula.text.isPunctuationMark() && secondAfterFormula == nextFormula.text.first())
		) {
			return@rule emptyList()
		}

		val indicator = """[A-ZА-Я].*?""".toRegex()
		if (indicator.matches(firstAfterFormula.text)) {
			return@rule if (lastFormulaSymbol != PunctuationMark.FULL_STOP.value) violationLines else emptyList()
		}

		if (firstAfterFormula.text.isPunctuationMark() &&
			secondAfterFormula != null && indicator.matches(secondAfterFormula.text)
		) {
			return@rule if (firstAfterFormula.text.single() != PunctuationMark.FULL_STOP.value) violationLines
			else emptyList()
		}

		return@rule emptyList()
	}
	.getRule()

val commaAfterFormulaRule = FormulaPunctuationRuleBuilder()
	.called("Отсутствует запятая после формулы")
	.ignoredWords(*ignoringAfterFormula.toTypedArray())
	.rule { formula, filteredText, nextFormula ->
		val violationLines = listOf(formula.lines.last())
		val lastFormulaSymbol = formula.text.last().text.last()

		if (filteredText.isEmpty()) return@rule emptyList()

		val (firstAfterFormula, secondAfterFormula) = filteredText.first() to filteredText.getOrNull(1)
		if (nextFormula != null) {
			if (firstAfterFormula == nextFormula.text.first()) {
				return@rule if (lastFormulaSymbol != PunctuationMark.COMMA.value) violationLines else emptyList()
			}

			if (firstAfterFormula.text.isPunctuationMark() && secondAfterFormula == nextFormula.text.first()) {
				return@rule if (firstAfterFormula.text.single() != PunctuationMark.COMMA.value) violationLines
				else emptyList()
			}
		}

		val indicator = """где""".toRegex()
		if (indicator.matches(firstAfterFormula.text)) {
			return@rule if (lastFormulaSymbol != PunctuationMark.COMMA.value) violationLines else emptyList()
		}

		if (firstAfterFormula.text.isPunctuationMark() &&
			secondAfterFormula != null && indicator.matches(secondAfterFormula.text)
		) {
			return@rule if (firstAfterFormula.text.single() != PunctuationMark.COMMA.value) violationLines
			else emptyList()
		}

		return@rule emptyList()
	}
	.getRule()

val RULES_FORMULA_PUNCTUATION = listOf(fullStopAfterFormulaRule, commaAfterFormulaRule)
