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

package jodd.jerry;

import jodd.csselly.selector.PseudoClass;
import jodd.csselly.selector.PseudoClassSelector;
import jodd.csselly.selector.PseudoFunction;
import jodd.csselly.selector.PseudoFunctionSelector;
import jodd.lagarto.dom.Element;
import jodd.lagarto.dom.LagartoDOMBuilder;
import jodd.lagarto.dom.Node;
import jodd.lagarto.dom.NodeSelector;
import jodd.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

class JerryMiscTest {

	@Test
	void testTextContentDecoding() {
		final String html = "<html><body><div>&#1054;&#1076;&#1112;&#1072;&#1074;&#1080; &#1089;&#1077;</div></body></html>";

		final Jerry doc = Jerry.of(html);
		final Jerry div = doc.s("div");

		assertEquals(9, div.text().length());
		assertEquals("Одјави се", div.text());
		assertEquals(9, div.html().length());
	}

	@Test
	void testTextContentDecoding2() {
		final String html = "<html><body><div></div></body></html>";

		final Jerry doc = Jerry.of(html);
		final Jerry div = doc.s("div");
		assertEquals(0, div.text().length());
		div.text("Одјави се");
		assertEquals(9, div.text().length());
		assertEquals("Одјави се", div.text());
		assertEquals(9, div.html().length());
	}

	@Test
	void testAppend1() {
		final JerryParser jerryParser = Jerry.create();

		((LagartoDOMBuilder) jerryParser.getDOMBuilder()).enableHtmlMode();

		final Jerry doc = jerryParser.parse("<xml><book><name>Foo</name></book></xml>");

		final Jerry book = doc.s("book");

		book.append("<br>");

		assertEquals("<xml><book><name>Foo</name><br></book></xml>", doc.html());
	}

	@Test
	void testAppend2() {
		final JerryParser jerryParser = Jerry.create();

		((LagartoDOMBuilder) jerryParser.getDOMBuilder()).enableXmlMode();

		final Jerry doc = jerryParser.parse("<xml><book><name>Foo</name></book></xml>");

		final Jerry book = doc.s("book");

		book.append("<br>");

		assertEquals("<xml><book><name>Foo</name><br></br></book></xml>", doc.html());
	}

	@Test
	void testAppend3() {
		final JerryParser jerryParser = Jerry.create();

		((LagartoDOMBuilder) jerryParser.getDOMBuilder()).enableXhtmlMode();

		final Jerry doc = jerryParser.parse("<xml><book><name>Foo</name></book></xml>");

		final Jerry book = doc.s("book");

		book.append("<br>");

		assertEquals("<xml><book><name>Foo</name><br/></book></xml>", doc.html());
	}

	@Test
	void testNullForEmpty() {
		final Jerry doc = Jerry.create().parse("<html></html>");

		assertNull(doc.s("#not-a-valid-id").attr("someAttribute"));

		assertNull(doc.s("#not-a-valid-id").css("name"));

		assertNull(doc.s("#not-a-valid-id").html());
	}

	@Test
	void testFirstNotDirectly() {
		final Jerry doc = Jerry.create().parse("<html><div>one</div><p>two</p><div>three</div><p>four</p></html>");

		assertEquals(2, doc.s("div").size());
		assertEquals(2, doc.s("p").size());
		assertEquals("one", doc.s("div").first().text());
		assertEquals("two", doc.s("p").first().text());

		assertEquals("four", doc.s("p").last().text());
		assertEquals("three", doc.s("div").last().text());
	}

	@Test
	void testIterator1() {
		final Jerry doc = Jerry.create().parse("<div id='one' class='foo'>one</div><div id='two' class='foo'>two</div>");

		final Iterator<Jerry> iterator = doc.find(".foo").iterator();
		String result = "";

		while (iterator.hasNext()) {
			final Jerry j = iterator.next();
			result += j.attr("id");
		}

		assertEquals("onetwo", result);
	}

	@Test
	void testIterator2() {
		final Jerry doc = Jerry.create().parse("<div id='one' class='foo'>one</div><div id='two' class='foo'>two</div>");

		final Iterator<Jerry> iterator = doc.find(".notfound").iterator();
		String result = "";

		while (iterator.hasNext()) {
			final Jerry j = iterator.next();
			result += j.attr("id");
		}

		assertEquals("", result);
	}

	@Test
	void testHtmlNodesOwner() {
		final Jerry doc = Jerry.create().parse("<div>1<div id='x'>2</div>3</div>");

		doc.s("#x").html("<span>wow</span>");

		assertEquals("<div>1<div id=\"x\"><span>wow</span></div>3</div>", doc.html());

		final Element divx = doc.get(0).getChildElement(0).getChildElement(0);

		assertSame(doc.get(0), divx.getOwnerDocument());
		assertEquals("span", divx.getChildElement(0).getNodeName());

		assertSame(doc.get(0), divx.getChildElement(0).getOwnerDocument());
		assertSame(doc.get(0), divx.getChildElement(0).getChild(0).getOwnerDocument());
	}

	@Test
	void testContains() {
		final Jerry doc = Jerry.create().parse("<body>aaa<p>foo 401(k) bar</p>xxx</body>");

		Jerry p = doc.s("p:contains('401(k)')");
		assertEquals(1, p.size());

		p = doc.s("p:contains('402(k)')");
		assertEquals(0, p.size());
	}

	@Test
	void testCustomPseudoClass() {
		PseudoClassSelector.registerPseudoClass(MyPseudoClass.class);

		final Jerry doc = Jerry.create().parse("<body><p jodd-attr='1'>found</p><p>not found</p></body>");

		final Jerry p = doc.s("p:jjjjj");
		assertEquals(1, p.size());
		assertEquals("found", p.text());
	}

	public static class MyPseudoClass extends PseudoClass {
		@Override
		public boolean match(final Node node) {
			return node.hasAttribute("jodd-attr");
		}

		@Override
		public String getPseudoClassName() {
			return "jjjjj";
		}
	}

	@Test
	void testCustomPseudoFunction() {
		PseudoFunctionSelector.registerPseudoFunction(MyPseudoFunction.class);

		final Jerry doc = Jerry.create().parse("<body><p>not found</p><div>This!</div></body>");

		final Jerry p = doc.s(":super-fn(3)");
		assertEquals(1, p.size());
		assertEquals("This!", p.text());
	}

	public static class MyPseudoFunction extends PseudoFunction {
		@Override
		public Object parseExpression(final String expression) {
			return Integer.valueOf(expression);
		}

		@Override
		public boolean match(final Node node, final Object expression) {
			final Integer size = (Integer) expression;
			return node.getNodeName().length() == size.intValue();
		}

		@Override
		public String getPseudoFunctionName() {
			return "super-fn";
		}
	}

	@Test
	void testCreateElementError() {
		final Jerry j = Jerry.of("1<span>2</span>3<span></span>4");

		j.attr("id", "test");
		assertEquals("1<span>2</span>3<span></span>4", j.html());

		j.s("*").attr("id", "test");

		assertEquals("1<span id=\"test\">2</span>3<span id=\"test\"></span>4", j.html());
	}

	@Test
	void testCustomerDetails() {
		final Jerry doc = Jerry.of("<p>to<br>{customerDetails}</p>");

		doc.s("p").each(($this, index) -> {
			String innerHtml = $this.html();
			innerHtml = StringUtil.replace(innerHtml, "{customerDetails}", "Jodd <b>rocks</b>");
			$this.html(innerHtml);
			return true;
		});

		final String newHtml = doc.html();
		assertEquals("<p>to<br>Jodd <b>rocks</b></p>", newHtml);
	}

	@Test
	void testNull() {
		String html = null;
		Jerry jerry = Jerry.of(html);

		assertEquals(1, jerry.nodes.length);
		assertEquals(0, jerry.nodes[0].getChildNodes().length);

		html = "";
		jerry = Jerry.of(html);

		assertEquals(1, jerry.nodes.length);
		assertEquals(0, jerry.nodes[0].getChildNodes().length);
	}

	@Test
	void test233() {
		final String html = "<div><span>name</span>value</div>";

		final Jerry $ = Jerry.of(html);

		assertEquals("namevalue", $.text());

		assertEquals(1, $.children().size());

		final Node div = $.children().get(0);

		assertEquals("div", div.getNodeName());

		assertEquals(2, div.getChildNodesCount());

		assertEquals("value", div.getChild(1).getNodeValue());
	}

	@Test
	void testEmptyClassAttribute() {
		final Jerry doc = Jerry.of("<div class></div>");

		try {
			doc.find(".foo");
		} catch (final Exception e) {
			fail(e.toString());
		}
	}

	@Test
	void test250() {
		final String html = "<html>\n" +
				"  <body>\n" +
				"    <a href=\"/go?to=foobar&index=null\" title=\"Choice 1\">link</a>\n" +
				"  </body>\n" +
				"</html>";

		final LagartoDOMBuilder domBuilder = new LagartoDOMBuilder();
		final NodeSelector nodeSelector = new NodeSelector(domBuilder.parse(html));
		final List<Node> selectedNodes = nodeSelector.select("a[title='Choice 1']");

		System.out.println();

		assertEquals("/go?to=foobar&index=null", selectedNodes.get(0).getAttribute("href"));
	}

	@Test
	void test279() {
		final String html = "<html><body><div>x</div></body></html>";

		final Jerry $ = Jerry.of(html);

		$.s("body").html("");
		assertEquals("<html><body></body></html>", $.html());

		$.s("body").append("");
		assertEquals("<html><body></body></html>", $.html());

		$.s("body").before("");
		assertEquals("<html><body></body></html>", $.html());
	}

	@Test
	void test321() {
		final String html = "<head><title>test &amp; blah</title><body><h1>test &amp; blah<b>bold</b></h1></body>";

		final Jerry doc = Jerry.of(html);
		final Jerry title = doc.s("title");

		assertEquals("test &amp; blah", title.eq(0).html());
		assertEquals("test & blah", title.eq(0).text());

		final Jerry h1 = doc.s("h1");
		assertEquals("test &amp; blah<b>bold</b>", h1.eq(0).html());
		assertEquals("test & blahbold", h1.eq(0).text());
	}

}
