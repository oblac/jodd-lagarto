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
