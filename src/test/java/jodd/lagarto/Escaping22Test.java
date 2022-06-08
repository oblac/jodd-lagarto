package jodd.lagarto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Escaping22Test {
	@Test
	void testEscapedFlag() {

		final StringBuilder sb = new StringBuilder();
		final EmptyTagVisitor visitor = new EmptyTagVisitor() {
			@Override
			public void text(final CharSequence text) {
				sb.append(text);
			}

		};

		final String html = "<html><body>\n" +
				"A&zwnj;B\n" +
				"C&nbsp;D\n" +
				"</body></html>";

		final LagartoParser parser = new LagartoParser(html);
		parser.parse(visitor);
		assertEquals("\nA\u200CB\nC\u00A0D\n", sb.toString());

		final LagartoParserConfig config = new LagartoParserConfig().setDecodeHtmlEntities(false);
		final LagartoParser parser2 = new LagartoParser(config, html);
		sb.setLength(0);
		parser2.parse(visitor);
		assertEquals("\nA&zwnj;B\nC&nbsp;D\n", sb.toString());
	}

}
