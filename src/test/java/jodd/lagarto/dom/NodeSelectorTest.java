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

import jodd.csselly.CSSelly;
import jodd.csselly.CSSellyException;
import jodd.csselly.CssSelector;
import jodd.io.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeSelectorTest {

	protected final String testDataRoot = this.getClass().getResource("data").getFile();
	
	@Test
	void testTags() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select("div");
		assertEquals(5, nodes.size());

		nodes = nodeSelector.select("body");
		assertEquals(1, nodes.size());
		assertEquals("body", nodes.get(0).getNodeName());

		nodes = nodeSelector.select("p");
		assertEquals(4, nodes.size());
	}

	@Test
	void testMoreTags() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select("div b");
		assertEquals(1, nodes.size());
		assertEquals("b", nodes.get(0).getNodeName());
		assertEquals("p", nodes.get(0).getParentNode().getNodeName());

		nodes = nodeSelector.select("p b");
		assertEquals(4, nodes.size());

		nodes = nodeSelector.select("div div");
		assertEquals(3, nodes.size());

		nodes = nodeSelector.select("div div div");
		assertEquals(2, nodes.size());

		nodes = nodeSelector.select("* div div div");
		assertEquals(2, nodes.size());
	}

	@Test
	void testIdClass() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select("div#fiona");
		assertEquals(1, nodes.size());
		assertEquals("fiona", nodes.get(0).getAttribute("id"));

		nodes = nodeSelector.select("div#fiona div#jodd");
		assertEquals(1, nodes.size());
		assertEquals("jodd", nodes.get(0).getAttribute("id"));

		nodes = nodeSelector.select("div.k1");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("div.k2");
		assertEquals(2, nodes.size());

		nodes = nodeSelector.select("div.k1.k2");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select(".k1.k2");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("p em");
		assertEquals(5, nodes.size());

		nodes = nodeSelector.select("p * em");
		assertEquals(2, nodes.size());
	}

	@Test
	void testAttributes() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select("div[id]");
		assertEquals(2, nodes.size());

		nodes = nodeSelector.select("html body div[id] div#jodd");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("div[id*=ion]");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("div[id*=o]");
		assertEquals(2, nodes.size());

		nodes = nodeSelector.select("div[id$=odd]");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("div[id$=od]");
		assertEquals(0, nodes.size());

		nodes = nodeSelector.select("div[id^=jo]");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("div[id^=od]");
		assertEquals(0, nodes.size());

		nodes = nodeSelector.select("[lang|=en]");
		assertEquals(1, nodes.size());
		assertEquals("h1", nodes.get(0).getNodeName());

		nodes = nodeSelector.select("[class~=k1]");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("[class~=k2]");
		assertEquals(2, nodes.size());
		nodes = nodeSelector.select("[class~=k2][class~=k1]");
		assertEquals(1, nodes.size());
	}

	@Test
	void testCombinators() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select("p#text > span");
		assertEquals(1, nodes.size());
		Node spanNode = nodes.get(0);
		assertEquals("spanner", (spanNode.getChild(0)).getNodeValue());

		nodes = nodeSelector.select("p#text > em");
		assertEquals(3, nodes.size());

		nodes = nodeSelector.select("p#text > em#oleg + em");
		assertEquals(0, nodes.size());
		nodes = nodeSelector.select("p#text > em#oleg + span");
		assertEquals(1, nodes.size());
		assertEquals("spanner", (nodes.get(0).getChild(0)).getNodeValue());

		nodes = nodeSelector.select("p#text > em#oleg ~ em");
		assertEquals(1, nodes.size());
		assertEquals("lina", nodes.get(0).getAttribute(0).getValue());
	}

	@Test
	void testPseudoClasses() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select("p#text > em:first-child");
		assertEquals(1, nodes.size());
		assertEquals("ema", nodes.get(0).getAttribute(0).getValue());

		nodes = nodeSelector.select("p#text  em:first-child");
		assertEquals(3, nodes.size());

		nodes = nodeSelector.select("p#text > em:last-child");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("p#text em:last-child");
		assertEquals(2, nodes.size());

		nodes = nodeSelector.select("em:only-child");
		assertEquals(1, nodes.size());
		assertEquals("Sanja", (nodes.get(0).getFirstChild()).getNodeValue());

		nodes = nodeSelector.select("em:first-child:last-child");
		assertEquals(1, nodes.size());
		assertEquals("Sanja", (nodes.get(0).getFirstChild()).getNodeValue());

		nodes = nodeSelector.select("b:first-of-type");
		assertEquals(3, nodes.size());

		nodes = nodeSelector.select("p#text b:first-of-type");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("p#text b:last-of-type");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("p:root");
		assertEquals(0, nodes.size());
		nodes = nodeSelector.select("html:root");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select(":empty");
		assertEquals(2, nodes.size());

		nodes = nodeSelector.select("b span:only-of-type");
		assertEquals(1, nodes.size());
		assertEquals("framework", (nodes.get(0).getFirstChild()).getNodeValue());

	}

	@Test
	void testPseudoFunctions() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select("p#text > em:nth-child(2n+1)");
		assertEquals(2, nodes.size());

		nodes = nodeSelector.select("p#text  em:nth-child(2n+1)");
		assertEquals(4, nodes.size());

		nodes = nodeSelector.select("p#text > em:nth-last-child(2n+1)");
		assertEquals(1, nodes.size());
		assertEquals("lina", (nodes.get(0)).getAttribute("id"));

		nodes = nodeSelector.select("p#text em:nth-last-child(2n+1)");
		assertEquals(2, nodes.size());

		nodes = nodeSelector.select("p#text em:nth-of-type(odd)");
		assertEquals(4, nodes.size());

		nodes = nodeSelector.select("p#text em:nth-of-type(even)");
		assertEquals(1, nodes.size());

		nodes = nodeSelector.select("p#text em:nth-last-of-type(odd)");
		assertEquals(4, nodes.size());

		nodes = nodeSelector.select("p#text em:nth-last-of-type(even)");
		assertEquals(1, nodes.size());

	}

	@Test
	void testDuplicatesRemoval() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select("div div");
		assertEquals(3, nodes.size());
	}


	@Test
	void testNodeSelector() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<Node> nodes = nodeSelector.select(new NodeFilter() {
			@Override
			public boolean accept(Node node) {
				if (node.getNodeType() != Node.NodeType.ELEMENT) {
					return false;
				}
				if ("ema".equals(node.getAttribute("id"))) {
					return true;
				}
				if ("lina".equals(node.getAttribute("id"))) {
					return true;
				}
				return false;
			}
		});

		assertEquals(2, nodes.size());
	}

	@Test
	void testTwoHtml() throws IOException {
		File file = new File(testDataRoot, "two.html");
		String htmlContent = FileUtil.readString(file);

		Document document = new LagartoDOMBuilder().parse(htmlContent);

		Node html = new NodeSelector(document).select("html").get(0);
		assertNotNull(html);

		Node body = new NodeSelector(html).selectFirst("body");
		Element h1 = body.getFirstChildElement();
		assertEquals("h1", h1.getNodeName());

		Node comment1 = body.getFirstChild().getNextSibling();
		assertEquals(Node.NodeType.COMMENT, comment1.getNodeType());


		Element p = (Element) new NodeSelector(body).selectFirst("p");

		assertEquals(h1, p.getPreviousSiblingElement());
		assertEquals(h1, comment1.getNextSiblingElement());
		assertNull(comment1.getNextSiblingName());

		// check if filter works just for sub elements
		List<Node> p_ems = new NodeSelector(p).select("em");
		assertEquals(1, p_ems.size());

		Element script = (Element) new NodeSelector(html).selectFirst("script");
		assertEquals("text/javascript", script.getAttribute("type"));

		assertTrue(document.check());
	}

	@Test
	void testGroupOfSelectors() throws IOException {
		File file = new File(testDataRoot, "one.html");
		String htmlContent = FileUtil.readString(file);

		Document document = new LagartoDOMBuilder().parse(htmlContent);

		List<Node> nodes = new NodeSelector(document).select("em, b, b");
		assertEquals(9, nodes.size());

		assertTrue(document.check());
	}
	
	@Test
	void testClassWithTabs() throws IOException {
		File file = new File(testDataRoot, "class-tabs.html");
		String htmlContent = FileUtil.readString(file);

		Document document = new LagartoDOMBuilder().parse(htmlContent);

		List<Node> nodes = new NodeSelector(document).select(".hey");
		assertEquals(1, nodes.size());

		Node n = nodes.get(0);
		assertEquals("div", n.getNodeName());
		assertEquals("jodd", n.getAttribute("id"));
	}

	@Test
	void testCollectionOfSelectors() throws IOException {
		NodeSelector nodeSelector = createNodeFilter();

		List<CssSelector> selectors1 = new CSSelly("body").parse();
		List<CssSelector> selectors2 = new CSSelly("p").parse();

		List<List<CssSelector>> collection = new ArrayList<>();
		collection.add(selectors1);
		collection.add(selectors2);

		List<Node> nodes = nodeSelector.select(collection);

		assertEquals(5, nodes.size());

		assertEquals("body", nodes.get(0).nodeName);
	}

	@Test
	void testAttributeValueWhitespaces() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse("<p id='p1' title='Foo Bar'></p><p id='p2' title=' Foo Bar '></p>"));

		// whitespace around the value is ignored
		assertEquals("p1", nodeSelector.selectFirst("[title= 'Foo Bar' ]").getAttribute("id"));

		// whitespace inside quotes is significant
		List<Node> nodes = nodeSelector.select("[title=' Foo Bar ']");
		assertEquals(1, nodes.size());
		assertEquals("p2", nodes.get(0).getAttribute("id"));
		assertEquals(0, nodeSelector.select("[title='Foo Bar ']").size());
	}

	@Test
	void testTypeSelectorCase() {
		final String html = "<div id='d1'></div><DIV id='d2'></DIV><svg><linearGradient id='g1'/></svg>";

		// HTML: type selectors are matched case-insensitively
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse(html));
		assertEquals(2, nodeSelector.select("DIV").size());
		assertEquals(2, nodeSelector.select("div").size());
		assertEquals(2, nodeSelector.select("Div").size());

		// XML: type selectors are matched case-sensitively
		nodeSelector = new NodeSelector(new LagartoDOMBuilder().enableXmlMode().parse(html));
		assertEquals(1, nodeSelector.select("DIV").size());
		assertEquals("d2", nodeSelector.select("DIV").get(0).getAttribute("id"));
		assertEquals(1, nodeSelector.select("linearGradient").size());
		assertEquals(0, nodeSelector.select("lineargradient").size());
	}

	@Test
	void testEmpty() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse(
			"<p id='p1'></p><p id='p2'><!-- comment --></p><p id='p3'> </p><p id='p4'>text</p><p id='p5'><b></b></p>"));

		List<Node> nodes = nodeSelector.select("p:empty");

		// comments don't affect emptiness, whitespace does
		assertEquals(2, nodes.size());
		assertEquals("p1", nodes.get(0).getAttribute("id"));
		assertEquals("p2", nodes.get(1).getAttribute("id"));
	}

	@Test
	void testContainsWithEscapes() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse("<p id='p1'>Hello (there)</p><p id='p2'>it's</p>"));

		assertEquals("p1", nodeSelector.selectFirst("p:contains(Hello \\(there\\))").getAttribute("id"));
		assertEquals("p1", nodeSelector.selectFirst("p:contains(Hello \\28 there\\29)").getAttribute("id"));
		assertEquals("p2", nodeSelector.selectFirst("p:contains('it\\'s')").getAttribute("id"));
		assertEquals(0, nodeSelector.select("p:contains(Hello \\(here\\))").size());
	}

	@Test
	void testContainsIgnoresCase() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse("<p id='p1'>Hello <b>World</b></p><p id='p2'>other</p>"));

		assertEquals("p1", nodeSelector.selectFirst("p:contains(hello world)").getAttribute("id"));
		assertEquals("p1", nodeSelector.selectFirst("p:contains('HELLO')").getAttribute("id"));
		assertEquals("p1", nodeSelector.selectFirst("p:contains(\\57 orld)").getAttribute("id"));
		assertEquals(1, nodeSelector.select("p:contains(OTHER)").size());
		assertEquals(0, nodeSelector.select("p:contains(others)").size());
	}

	@Test
	void testContainsNormalizesText() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse(
			"<div id='d1'><p>Hello\n   World</p></div><ul id='u1'><li>a</li><li>b</li></ul>" +
			"<div id='d2'><script>var secret;</script><style>.hidden {}</style>visible</div>"));

		assertEquals(2, nodeSelector.select(":contains(hello world)").size());
		assertEquals(1, nodeSelector.select("p:contains('Hello   World')").size());
		assertEquals("u1", nodeSelector.selectFirst("ul:contains(a b)").getAttribute("id"));
		assertEquals(0, nodeSelector.select("ul:contains(ab)").size());
		assertEquals(0, nodeSelector.select("div:contains(secret)").size());
		assertEquals(0, nodeSelector.select("div:contains(hidden)").size());
		assertEquals("d2", nodeSelector.selectFirst("div:contains(visible)").getAttribute("id"));
	}

	@Test
	void testFormAndLinkPseudoClasses() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse(
			"<form>" +
			"<input id='i1' required><input id='i2' disabled><input id='i3' readonly><input id='i4' type='hidden' required>" +
			"<input id='i5' type='checkbox'><input id='i6' type='unknown'>" +
			"<fieldset id='f1' disabled><legend><input id='i7'></legend><legend><input id='i8'></legend>" +
			"<fieldset id='f2'><button id='b1'>b</button></fieldset></fieldset>" +
			"<select id='s1' required><optgroup id='g1' disabled><option id='o1'>a</option></optgroup>" +
			"<option id='o2' disabled>b</option><option id='o3'>c</option></select>" +
			"<textarea id='t1'></textarea><textarea id='t2' readonly></textarea>" +
			"</form>" +
			"<div id='d1' contenteditable><p id='p1'>x</p><p id='p2' contenteditable='false'>y</p></div>" +
			"<a id='a1' href='/'>a</a><a id='a2' name='n'>b</a><map><area id='r1' href='/'></map><link id='l1' href='/'>"));

		// expected values are the ones of Chrome
		assertEquals("i1,i3,i4,i5,i6,i7,s1,o3,t1,t2", ids(nodeSelector.select(":enabled")));
		assertEquals("i2,f1,i8,f2,b1,g1,o1,o2", ids(nodeSelector.select(":disabled")));
		assertEquals("i1,s1", ids(nodeSelector.select(":required")));
		assertEquals("i2,i3,i4,i5,i6,i7,i8,b1,t1,t2", ids(nodeSelector.select(":optional")));
		assertEquals("i1,i6,i7,t1,d1,p1", ids(nodeSelector.select(":read-write")));
		assertEquals("i2,i3,i8", ids(nodeSelector.select("input:read-only:not([type])")));
		assertEquals("a1,r1", ids(nodeSelector.select(":link")));
		assertEquals("a1,r1", ids(nodeSelector.select(":any-link")));
	}

	@Test
	void testNthChildOfSelector() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse(
			"<ul><li id='l1' class='a'>1</li><li id='l2'>2</li><li id='l3' class='a'>3</li><p id='p1' class='a'>p</p>" +
			"<li id='l4' class='a b'>4</li><li id='l5'>5</li></ul>"));

		// expected values are the ones of Chrome
		assertEquals("l3", ids(nodeSelector.select("li:nth-child(2 of .a)")));
		// keywords are case-insensitive in CSS, even if Chrome only accepts a lowercase 'of'
		assertEquals("l3", ids(nodeSelector.select(":nth-child(2 OF li.a)")));
		assertEquals("l1,l4", ids(nodeSelector.select(":nth-child(odd of li.a)")));
		assertEquals("l2,l4", ids(nodeSelector.select(":nth-child(-n+2 of .b, li:not(.a))")));
		assertEquals("p1", ids(nodeSelector.select(":nth-child(3 of .a)")));
		assertEquals("l4", ids(nodeSelector.select(":nth-last-child(1 of .a)")));
		assertEquals("l4", ids(nodeSelector.select(":nth-last-child(2 of .a, #l5)")));
		// without 'of', all the children are counted
		assertEquals("l2", ids(nodeSelector.select(":nth-child(2)")));

		assertThrows(CSSellyException.class, () -> nodeSelector.select(":nth-child(2 of )"));
		assertThrows(CSSellyException.class, () -> nodeSelector.select(":nth-child(2 of > li)"));
		assertThrows(CSSellyException.class, () -> nodeSelector.select(":nth-of-type(2 of .a)"));
	}

	@Test
	void testResultsInDocumentOrder() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse(
			"<div id='d1'><p id='p1'></p><div id='d2'><p id='p2'></p></div><p id='p3'></p></div>" +
			"<ul><li id='l1'></li><li id='l2' class='a'></li><li id='l3'></li><li id='l4' class='a'></li></ul>"));

		// the nested div used to be walked after its parent div
		assertEquals("p1,p2,p3", ids(nodeSelector.select("div > p")));
		assertEquals("p1,p2,p3", ids(nodeSelector.select("div p")));
		assertEquals("l2,l3,l4", ids(nodeSelector.select("li ~ li")));
		assertEquals("l3,l4", ids(nodeSelector.select(".a ~ li")));
		assertEquals("p1,p2,p3,l2,l4", ids(nodeSelector.select(".a, div > p")));
		// relative selectors, as used by :has()
		assertEquals("l2,l3,l4", ids(new NodeSelector(nodeSelector.selectFirst("#l1")).select("~ li")));
		assertEquals("d2,p2,p3", ids(new NodeSelector(nodeSelector.selectFirst("#p1")).select("~ * , ~ div p")));
		// jQuery positional pseudo-classes still apply to the current results
		assertEquals("l1", ids(nodeSelector.select("li:first")));
		assertEquals("l4", ids(nodeSelector.select("ul li:last")));
		assertEquals("l3", ids(nodeSelector.select("li:eq(2)")));
	}

	@Test
	void testTurkishLocale() {
		final java.util.Locale defaultLocale = java.util.Locale.getDefault();
		java.util.Locale.setDefault(new java.util.Locale("tr", "TR"));
		try {
			// 'I' is lower-cased to a dotless 'ı' in Turkish
			assertEquals("first-child", new jodd.csselly.selector.PseudoClasses.FIRST_CHILD().getPseudoClassName());
			assertEquals("nth-child", new jodd.csselly.selector.PseudoFunctions.NTH_CHILD().getPseudoFunctionName());

			NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse("<TITLE ID='t1'>t</TITLE><LI ID='l1'>i</LI>"));
			assertEquals("title", nodeSelector.selectFirst("#t1").getNodeName());
			assertEquals("t1", ids(nodeSelector.select("title[id=t1]")));
			assertEquals("l1", ids(nodeSelector.select("LI[ID]:last-child, li:nth-child(2)")));
		} finally {
			java.util.Locale.setDefault(defaultLocale);
		}
	}

	private static String ids(final List<Node> nodes) {
		final List<String> ids = new ArrayList<>();
		for (final Node node : nodes) {
			ids.add(node.getAttribute("id"));
		}
		return String.join(",", ids);
	}

	@Test
	void testChecked() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse(
			"<input id='c1' type='checkbox' checked><input id='c2' type='checkbox'><input id='r1' type='RADIO' checked>" +
			"<input id='t1' type='text' checked><div id='d1' checked></div>" +
			"<select><option id='o1'>a</option><option id='o2' selected>b</option></select>"));

		List<Node> nodes = nodeSelector.select(":checked");
		assertEquals(3, nodes.size());
		assertEquals("c1", nodes.get(0).getAttribute("id"));
		assertEquals("r1", nodes.get(1).getAttribute("id"));
		assertEquals("o2", nodes.get(2).getAttribute("id"));
		assertEquals("o2", nodeSelector.selectFirst("option:checked").getAttribute("id"));
	}

	@Test
	void testCaseInsensitivePseudoClasses() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse("<ul><li>1</li><li>2</li><li>3</li></ul>"));

		assertEquals(1, nodeSelector.select("li:FIRST-CHILD").size());
		assertEquals(1, nodeSelector.select("li:Last-Child").size());
		assertEquals(1, nodeSelector.select("UL:Has(> LI:First-Child)").size());
		assertEquals(2, nodeSelector.select("li:nth-child(ODD)").size());
		assertEquals(1, nodeSelector.select("li:nth-child(Even)").size());
		assertEquals(3, nodeSelector.select("li:nth-child(N)").size());
		assertEquals(2, nodeSelector.select("li:nth-child(2N+1)").size());
		assertEquals(2, nodeSelector.select("li:nth-last-child(-N+2)").size());
	}

	@Test
	void testClassesOnSeveralLines() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse("<p class='a\nb'>x</p><p class='c\r\n\td'>y</p>"));

		assertEquals(1, nodeSelector.select(".a").size());
		assertEquals(1, nodeSelector.select(".b").size());
		assertEquals(1, nodeSelector.select(".a.b").size());
		assertEquals(1, nodeSelector.select(".c.d").size());
		assertEquals(0, nodeSelector.select("[class~='a b']").size());
	}

	@Test
	void testHexEscapes() {
		NodeSelector nodeSelector = new NodeSelector(new LagartoDOMBuilder().parse(
			"<p id='1st' class='123'>a</p><p class='c:d' data-u='café' data-sp='foo bar' data-q=\"it's\">b</p><p class='a\\b'>c</p>"));

		// ids and classes that start with a digit, as copied from the browser dev tools
		assertEquals("1st", nodeSelector.selectFirst("#\\31 st").getAttribute("id"));
		assertEquals("1st", nodeSelector.selectFirst(".\\31 23").getAttribute("id"));
		assertEquals("1st", nodeSelector.selectFirst(".\\00003123").getAttribute("id"));
		assertEquals("1st", nodeSelector.selectFirst("[class=\\31 23]").getAttribute("id"));
		assertEquals(1, nodeSelector.select(".c\\:d").size());
		assertEquals(1, nodeSelector.select(".c\\3a d").size());
		assertEquals(1, nodeSelector.select(".c\\3A d").size());
		assertEquals(1, nodeSelector.select("[data-u=\"caf\\e9\"]").size());
		assertEquals(1, nodeSelector.select("[data-u='caf\\0000e9']").size());
		// the whitespace after a hex escape belongs to the escape, so this is the value "caf\u00e9i", not a flag
		assertEquals(0, nodeSelector.select("[data-u=caf\\e9 i]").size());
		assertEquals(1, nodeSelector.select("[data-u=CAF\\e9  i]").size());
		assertEquals(1, nodeSelector.select("[data-sp=\"foo\\20 bar\"]").size());
		assertEquals(1, nodeSelector.select("[data-sp=foo\\20 bar]").size());
		assertEquals(1, nodeSelector.select("[data-sp=foo\\ bar]").size());
		assertEquals(1, nodeSelector.select("[data-q='it\\'s']").size());
		assertEquals(1, nodeSelector.select("[data-q=it\\'s]").size());
		// an escaped backslash
		assertEquals(1, nodeSelector.select(".a\\\\b").size());
		assertEquals(0, nodeSelector.select(".ab").size());
	}

	// ---------------------------------------------------------------- utils

	private NodeSelector createNodeFilter() throws IOException {
		File file = new File(testDataRoot, "one.html");
		String html = FileUtil.readString(file);
		return new NodeSelector(new LagartoDOMBuilder().parse(html));
	}
}
