package jodd.lagarto;

import java.nio.CharBuffer;

/**
 * Implementation of the {@link CharsInput} over a {@code char[]}.
 */
final class CharArrayInput extends CharsInput {
	public static final CharSequence EMPTY_CHAR_SEQUENCE = CharBuffer.allocate(0);

	private final char[] input;

	CharArrayInput(final char[] input) {
		super(input.length);
		this.input = input;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final char charAt(final int index) {
		return input[index];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final char charAtNdx() {
		return input[ndx];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final CharSequence subSequence(final int from, final int to) {
		if (from == to) {
			return EMPTY_CHAR_SEQUENCE;
		}
		return CharBuffer.wrap(input, from, to - from);
	}

}
