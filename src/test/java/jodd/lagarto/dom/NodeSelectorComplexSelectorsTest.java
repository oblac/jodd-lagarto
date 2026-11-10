// Copyright (c) 2003-present, Jodd Team (http://jodd.org)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package jodd.lagarto.dom;

import jodd.csselly.CSSellyException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Selectors with nested selectors, relative selectors, selector lists and attribute flags.
 */
class NodeSelectorComplexSelectorsTest {

	private static final String HTML =
		"<div id='main' lang='en-US' data-x='Foo Bar' title='a,b'>" +
			"<p id='p1' class='intro'>Hello <b id='b1'>world</b></p>" +
			"<p id='p2'>Second <i id='i1'>para</i></p>" +
			"<span id='s1'>span</span>" +
		"</div>" +
		"<ul id='ul'><li id='li1'>1</li><li id='li2' class='x'>2</li><li id='li3'>3</li><li id='li4'>4</li></ul>" +
		"<select id='sel'><option id='o1'>One</option><option id='o2'>Two</option><option id='o3'>Three</option></select>" +
		"<a id='a1' hreflang='EN' href='/page.html'>link</a>";

	private static final Document DOC = new LagartoDOMBuilder().parse(HTML);

	private static String select(final String query) {
		return select(DOC, query);
	}

	private static String select(final Node root, final String query) {
		final List<Node> nodes = new NodeSelector(root).select(query);
		return nodes.stream().map(n -> n.getAttribute("id")).collect(Collectors.joining(","));
	}

	@Test
	void testNestedParenthesesInPseudoFunctions() {
		assertEquals("o2,o3", select("option:not(:nth-child(1))"));
		assertEquals("li1,li3", select("li:not(:nth-child(2n))"));
		assertEquals("ul", select("ul:has(li:nth-child(4))"));
		assertEquals("p2", select("p:not(:has(b))"));
		assertEquals("p2", select("p:has(:not(b))"));
		assertEquals("main", select("div:has(p:not(:has(i)))"));
	}

	@Test
	void testStringsInNestedPseudoFunctions() {
		// see https://github.com/oblac/jodd-lagarto/issues/23
		assertEquals("p1", select("p:has(b:contains(\"world\"))"));
		assertEquals("p1", select("p:has(b:contains('world'))"));
		// parentheses inside strings don't count
		assertEquals("", select("p:has(i:contains('(para'))"));
		assertEquals("", select("p:has(i:contains('para)'))"));
		assertEquals("p1", select("p:contains('Hello ')"));
	}

	@Test
	void testUnclosedPseudoFunction() {
		assertThrows(CSSellyException.class, () -> select("li:not(:nth-child(1)"));
	}

	@Test
	void testNewlinesInPseudoFunctions() {
		assertEquals("li1,li3", select("li:nth-child(2n\n+1)"));
		assertEquals("li1,li4", select("li:not(.x,\n:nth-child(\n3\n))"));
	}

	@Test
	void testUnclosedQuotesInPseudoFunctions() {
		final Document doc = new LagartoDOMBuilder().parse("<p id='p1'>don't</p><p id='p2'>it's</p><p id='p3'>none</p>");
		assertEquals("p1", select(doc, "p:contains(don't)"));
		assertEquals("p1,p3", select(doc, "p:contains(don't), p:contains(none)"));
		assertEquals("p2", select(doc, "p:not(:contains(don't), :contains(none))"));
	}

	@Test
	void testCommasInsideSelectors() {
		assertEquals("li3,li4", select("li:not(.x, :first-child)"));
		assertEquals("main", select("[title='a,b']"));
		assertEquals("main", select("div:has(b, i)"));
	}

	@Test
	void testNotWithCombinators() {
		assertEquals("", select("p:not(div > p)"));
		assertEquals("b1", select("b:not(ul b)"));
		assertEquals("p1,s1", select("#main > :not(p + p)"));
		assertEquals("p1,p2", select("#main > :not(p ~ span)"));
	}

	@Test
	void testNotWithDescendantCombinatorsInDeepDocument() {
		final StringBuilder html = new StringBuilder();
		for (int i = 0; i < 200; i++) {
			html.append("<div>");
		}
		html.append("<p id='p'>x</p>");
		for (int i = 0; i < 200; i++) {
			html.append("</div>");
		}
		final Document doc = new LagartoDOMBuilder().parse(html.toString());
		// without memoization, every combination of 5 ancestors among 200 would be tried
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			assertEquals("p", select(doc, "p:not(span div div div div p)"));
			assertEquals("", select(doc, "p:not(div div div div div p)"));
		});
	}

	@Test
	void testIsAndWhere() {
		assertEquals("li1,li2", select("li:is(.x, :first-child)"));
		assertEquals("p1", select("p:is(.intro)"));
		assertEquals("p1,p2,s1", select(":is(p, span)"));
		assertEquals("li2", select("li:where(.x)"));
		assertEquals("b1,i1", select(":is(div p) > *"));
	}

	@Test
	void testRelativeSelectorsInHas() {
		assertEquals("main", select("div:has(> p)"));
		assertEquals("", select("div:has(> b)"));
		assertEquals("li1", select("li:has(+ li.x)"));
		assertEquals("li1", select("li:has(~ li.x)"));
		assertEquals("p1", select("p:has(> b):not(.x)"));
	}

	@Test
	void testExplicitScopeNotSupportedInHas() {
		// :scope in :has() is the scoping root of the whole selection, not the element tested by :has()
		assertThrows(CSSellyException.class, () -> select("div:has(:scope > span)"));
	}

	@Test
	void testRelativeSelectorsNotSupportedInNotAndIs() {
		assertThrows(CSSellyException.class, () -> select("p:not(> b)"));
		assertThrows(CSSellyException.class, () -> select("p:is(> b)"));
	}

	@Test
	void testScope() {
		final Node main = new NodeSelector(DOC).selectFirst("#main");
		assertEquals("p1,p2,s1", select(main, "> *"));
		assertEquals("p1,p2,s1", select(main, ":scope > *"));
		assertEquals("p2", select(main, "> p + p"));
		assertEquals("p1,p2,s1", select(main, "div:scope > *"));
		assertEquals("", select(main, "ul:scope > *"));
		// only the descendants of the element are selected
		assertEquals("", select(main, ":scope"));
		assertThrows(CSSellyException.class, () -> select("div :scope"));
	}

	@Test
	void testScopeFromDocument() {
		// when selecting from a document, :scope is the root element, like in browsers
		final Document doc = new LagartoDOMBuilder().parse("<html id='html'><body id='body'><p id='p'>p</p></body></html>");
		assertEquals("html", select(doc, ":scope"));
		assertEquals("body", select(doc, ":scope > body"));
		assertEquals("", select(doc, ":scope > p"));
		assertEquals("body", select(doc, "> body"));
		assertEquals("p", select(doc, ":scope p"));
	}

	@Test
	void testAttributeCaseSensitivityFlags() {
		assertEquals("main", select("[data-x='foo bar' i]"));
		assertEquals("main", select("[data-x=\"FOO BAR\"i]"));
		assertEquals("", select("[data-x='foo bar' s]"));
		assertEquals("", select("[data-x='foo bar']"));
		assertEquals("a1", select("[hreflang=en i]"));
		assertEquals("a1", select("[href$='.HTML' i]"));
		assertEquals("main", select("[lang|=EN i]"));
	}

	private static final String TEXTS =
		"<div id='d1'>Hello   <b id='b1'>World</b>  again</div><div id='d2'>line<br>break</div><pre id='pre'>a   b</pre>" +
		"<p id='p1'>non&nbsp;breaking</p><p id='p2'>'quoted' a(b)c</p><script id='s1'>var hello = 1;</script>" +
		"<p id='p3'>Hello <span id='sp'>World</span></p>";

	@Test
	void testContainsOwn() {
		// same results as jsoup 1.24.1
		final Document doc = new LagartoDOMBuilder().parse(TEXTS);
		assertEquals("d1", select(doc, "div:containsOwn(hello again)"));
		assertEquals("d1", select(doc, "div:containsOwn(HELLO   AGAIN)"));
		assertEquals("", select(doc, "div:containsOwn(world)"));
		assertEquals("b1", select(doc, "b:containsOwn(world)"));
		assertEquals("d2", select(doc, "div:containsOwn(line break)"));
		// whitespace is preserved in pre, while the searched text is normalized
		assertEquals("", select(doc, "pre:containsOwn(a   b)"));
		assertEquals("", select(doc, "pre:containsOwn(a b)"));
		assertEquals("p1", select(doc, "p:containsOwn(non breaking)"));
		// the expression is taken as is, quotes included
		assertEquals("p2", select(doc, "p:containsOwn('quoted')"));
		assertEquals("p2", select(doc, "p:containsOwn(a\\(b\\)c)"));
		// script content is data, not text
		assertEquals("", select(doc, "script:containsOwn(hello)"));
		assertEquals("d1,p3", select(doc, "*:containsOwn(hello)"));
		assertEquals("", select(doc, "p:containsOwn(hello world)"));
		assertThrows(CSSellyException.class, () -> select(doc, "p:containsOwn()"));
		// pseudo function names are case-insensitive
		assertEquals("d1,p3", select(doc, "*:containsown(hello)"));
	}

	private static final String MORE_TEXTS = TEXTS +
		"<ul id='ul'><li id='li1'>one</li><li id='li2'>two</li></ul><span id='sp2'>in<div id='d3'>block</div>line</span>" +
		"<p id='p4'>x<input id='in'>y</p><table id='t'><tr id='tr'><td id='td1'>12</td><td id='td2'>34</td></tr></table>";

	@Test
	void testMatches() {
		// same results as jsoup 1.24.1
		final Document doc = new LagartoDOMBuilder().parse("<html><body id='body'>" + MORE_TEXTS + "</body></html>");
		assertEquals("d1", select(doc, "div:matches(Hello World again)"));
		assertEquals("d1", select(doc, "div:matches((?i)hello\\s+world)"));
		assertEquals("d2", select(doc, "*:matches(^line break$)"));
		// whitespace is preserved in pre
		assertEquals("pre", select(doc, "pre:matches(a   b)"));
		assertEquals("", select(doc, "pre:matches(^a b$)"));
		// a space separates blocks and boundaries such as inputs
		assertEquals("ul", select(doc, "ul:matches(^one two$)"));
		assertEquals("sp2", select(doc, "span:matches(in block line)"));
		assertEquals("", select(doc, "p:matches(xy)"));
		assertEquals("p4", select(doc, "p:matches(x y)"));
		assertEquals("t", select(doc, "table:matches(^12 34$)"));
		assertEquals("td1,td2", select(doc, "td:matches(\\d+)"));
		// the expression is taken as is
		assertEquals("p2", select(doc, "p:matches('quoted')"));
		assertEquals("p2", select(doc, "p:matches(a\\(b\\)c)"));
		// script content is data, not text
		assertEquals("", select(doc, "script:matches(hello)"));
		assertEquals("p1", select(doc, "p:matches(non breaking)"));
		assertEquals("body", select(doc, "body:matches(World again line break)"));
		assertThrows(CSSellyException.class, () -> select(doc, "p:matches([)"));
		assertThrows(CSSellyException.class, () -> select(doc, "p:matches()"));
	}

	@Test
	void testHtmlCaseInsensitiveAttributeValues() {
		final String html = "<input id='i1' type='TEXT'><a id='a1' rel='NoFollow' title='Foo' hreflang='EN'>link</a>";
		final Document doc = new LagartoDOMBuilder().parse(html);

		// HTML lists attributes whose values are case-insensitive, like browsers
		assertEquals("i1", select(doc, "[type=text]"));
		assertEquals("i1", select(doc, "[TYPE=text]"));
		assertEquals("a1", select(doc, "[rel=nofollow]"));
		assertEquals("a1", select(doc, "[hreflang|=en]"));
		assertEquals("a1", select(doc, "[rel^=no]"));
		// unless the s flag is used
		assertEquals("", select(doc, "[type=text s]"));
		assertEquals("i1", select(doc, "[type=TEXT s]"));
		// other attributes are case-sensitive
		assertEquals("", select(doc, "[title=foo]"));
		assertEquals("a1", select(doc, "[title=foo i]"));

		// XML is case-sensitive
		final Document xml = new LagartoDOMBuilder().enableXmlMode().parse(html);
		assertEquals("", select(xml, "[type=text]"));
		assertEquals("i1", select(xml, "[type=text i]"));
	}

	@Test
	void testAttributeNotEquals() {
		// same results as jQuery 3.7.1
		final Document doc = new LagartoDOMBuilder().parse(
			"<a id='a1' rel='nofollow'>1</a><a id='a2' rel='me'>2</a><a id='a3'>3</a><a id='a4' rel='NoFollow'>4</a><a id='a5' rel=''>5</a>" +
			"<input id='i1' type='TEXT'><input id='i2' type='text'>");
		assertEquals("a2,a3,a4,a5", select(doc, "a[rel!=nofollow]"));
		assertEquals("a2,a3,a4,a5", select(doc, "a[rel!='nofollow']"));
		assertEquals("a1,a2,a3,a4", select(doc, "a[rel!='']"));
		assertEquals("a2,a4,a5", select(doc, "a[rel!=nofollow][rel]"));
		// always case-sensitive, even for the attributes HTML matches case-insensitively
		assertEquals("i1", select(doc, "input[type!=text]"));
		assertEquals("a2,a3,a5", select(doc, "a:not([rel=nofollow])"));
		// no flags
		assertThrows(CSSellyException.class, () -> select(doc, "a[rel!=nofollow i]"));
	}

	@Test
	void testMatchesOwn() {
		// same results as jsoup 1.24.1
		final Document doc = new LagartoDOMBuilder().parse("<html><body id='body'>" + MORE_TEXTS + "</body></html>");
		assertEquals("d1", select(doc, "div:matchesOwn(^Hello again$)"));
		assertEquals("", select(doc, "div:matchesOwn(World)"));
		assertEquals("b1", select(doc, "b:matchesOwn(World)"));
		assertEquals("d2", select(doc, "*:matchesOwn(^line break$)"));
		assertEquals("pre", select(doc, "pre:matchesOwn(a   b)"));
		// no separator for children elements in the own text
		assertEquals("", select(doc, "span:matchesOwn(^in line$)"));
		assertEquals("sp2", select(doc, "span:matchesOwn(^inline$)"));
		assertEquals("", select(doc, "span:matchesOwn(block)"));
		assertEquals("", select(doc, "p:matchesOwn(^x y$)"));
		assertEquals("p4", select(doc, "p:matchesOwn(^xy$)"));
		assertEquals("td1,td2", select(doc, "td:matchesOwn(\\d+)"));
		assertEquals("", select(doc, "tr:matchesOwn(\\d+)"));
		assertEquals("", select(doc, "script:matchesOwn(hello)"));
		assertEquals("p3", select(doc, "p:matchesOwn((?i)^HELLO$)"));
		assertThrows(CSSellyException.class, () -> select(doc, "p:matchesOwn()"));
	}

	@Test
	void testSelectorsGroupInDocumentOrder() {
		assertEquals("p1,li3", select("li.x + li, p:first-child"));
		assertEquals("main,ul,li1", select("li:first-child, ul, div"));
	}
}
