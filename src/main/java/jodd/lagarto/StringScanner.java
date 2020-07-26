package jodd.lagarto;

/**
 * Implementation of {@link Scanner} over the {@code String}.
 */
final class StringScanner extends Scanner {

	private final CharSequence input;

	StringScanner(final CharSequence input) {
		super(input.length());
		this.input = input;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final char charAt(final int index) {
		return input.charAt(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final char charAtNdx() {
		return input.charAt(ndx);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final CharSequence subSequence(final int start, final int end) {
		return input.subSequence(start, end);
	}
}
