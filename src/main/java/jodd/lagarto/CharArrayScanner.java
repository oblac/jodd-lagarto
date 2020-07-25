package jodd.lagarto;

import java.nio.CharBuffer;

final class CharArrayScanner extends Scanner {
	private static final CharSequence EMPTY_CHAR_SEQUENCE = CharBuffer.allocate(0);

	private final char[] input;

	CharArrayScanner(final char[] input) {
		super(input.length);
		this.input = input;
	}

	@Override
	public final char charAt(final int index) {
		return input[index];
	}

	@Override
	public final char charAtNdx() {
		return input[ndx];
	}

	/**
	 * Creates char sub-sequence from the input.
	 */
	@Override
	public final CharSequence subSequence(final int from, final int to) {
		if (from == to) {
			return EMPTY_CHAR_SEQUENCE;
		}
		return CharBuffer.wrap(input, from, to - from);
	}

}
