package jodd.jerry;

import jodd.lagarto.dom.DOMBuilder;
import jodd.lagarto.dom.Document;
import jodd.lagarto.dom.LagartoDOMBuilder;

/**
 * Content parser and Jerry factory.
 */
public class JerryParser {
	private static final String EMPTY = "";

	protected final DOMBuilder domBuilder;

	public JerryParser() {
		this(new LagartoDOMBuilder());
	}

	public JerryParser(final DOMBuilder domBuilder) {
		this.domBuilder = domBuilder;
	}

	/**
	 * Returns {@link DOMBuilder} for additional configuration.
	 */
	public DOMBuilder getDOMBuilder() {
		return domBuilder;
	}

	/**
	 * Invokes parsing on Jerry {@link DOMBuilder}.
	 */
	public Jerry parse(final char[] content) {
		final Document doc = domBuilder.parse(content);
		return new Jerry(domBuilder, doc);
	}

	/**
	 * Invokes parsing on Jerry {@link DOMBuilder}.
	 */
	public Jerry parse(CharSequence content) {
		if (content == null) {
			content = EMPTY;
		}
		final Document doc = domBuilder.parse(content);
		return new Jerry(domBuilder, doc);
	}
}
