package jodd.lagarto;

final class StringScanner extends Scanner {

	private final String input;

	public StringScanner(final String input) {
		super(input.length());
		this.input = input;
	}

	@Override
	public final char charAt(final int index) {
		return input.charAt(index);
	}

	@Override
	public final char charAtNdx() {
		return input.charAt(ndx);
	}

	@Override
	public final CharSequence subSequence(final int start, final int end) {
		return input.subSequence(start, end);
	}
}
