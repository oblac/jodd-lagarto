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
