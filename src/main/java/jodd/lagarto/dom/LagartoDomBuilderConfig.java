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

import jodd.lagarto.LagartoParserConfig;
import jodd.lagarto.dom.render.LagartoHtmlRenderer;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Additional configuration for {@link jodd.lagarto.dom.LagartoDOMBuilder}
 * based on {@link jodd.lagarto.LagartoParserConfig}.
 */
public class LagartoDomBuilderConfig {

	protected boolean ignoreWhitespacesBetweenTags;
	protected boolean ignoreComments;
	protected boolean selfCloseVoidTags;
	protected float condCommentIEVersion = 10;
	protected boolean enabledVoidTags = true;
	protected boolean impliedEndTags;

	protected boolean useFosterRules;
	protected boolean unclosedTagAsOrphanCheck;

	protected LagartoHtmlRenderer htmlRenderer = new LagartoHtmlRenderer();
	protected LagartoParserConfig parserConfig = new LagartoParserConfig();

	protected boolean collectErrors;
	protected boolean errorLogEnabled = true;
	protected BiConsumer<Logger, String> errorLogConsumer = Logger::error;


	// ---------------------------------------------------------------- access

	public boolean isUnclosedTagAsOrphanCheck() {
		return unclosedTagAsOrphanCheck;
	}

	public LagartoDomBuilderConfig setUnclosedTagAsOrphanCheck(final boolean unclosedTagAsOrphanCheck) {
		this.unclosedTagAsOrphanCheck = unclosedTagAsOrphanCheck;
		return this;
	}

	/**
	 * Returns {@code true} if {@link HtmlFosterRules foster rules}
	 * should be used.
	 */
	public boolean isUseFosterRules() {
		return useFosterRules;
	}

	public LagartoDomBuilderConfig setUseFosterRules(final boolean useFosterRules) {
		this.useFosterRules = useFosterRules;
		return this;
	}

	public boolean isIgnoreWhitespacesBetweenTags() {
		return ignoreWhitespacesBetweenTags;
	}

	/**
	 * Specifies if whitespaces between open/closed tags should be ignored.
	 */
	public LagartoDomBuilderConfig setIgnoreWhitespacesBetweenTags(final boolean ignoreWhitespacesBetweenTags) {
		this.ignoreWhitespacesBetweenTags = ignoreWhitespacesBetweenTags;
		return this;
	}

	public boolean isIgnoreComments() {
		return ignoreComments;
	}

	/**
	 * Specifies if comments should be ignored in DOM tree.
	 */
	public LagartoDomBuilderConfig setIgnoreComments(final boolean ignoreComments) {
		this.ignoreComments = ignoreComments;
		return this;
	}

	public boolean isEnabledVoidTags() {
		return enabledVoidTags;
	}

	/**
	 * Enables usage of void tags.
	 */
	public LagartoDomBuilderConfig setEnabledVoidTags(final boolean enabledVoidTags) {
		this.enabledVoidTags = enabledVoidTags;
		return this;
	}

	public boolean isSelfCloseVoidTags() {
		return selfCloseVoidTags;
	}

	/**
	 * Specifies if void tags should be self closed.
	 */
	public LagartoDomBuilderConfig setSelfCloseVoidTags(final boolean selfCloseVoidTags) {
		this.selfCloseVoidTags = selfCloseVoidTags;
		return this;
	}

	public boolean isCollectErrors() {
		return collectErrors;
	}

	/**
	 * Enables error collection during parsing.
	 */
	public LagartoDomBuilderConfig setCollectErrors(final boolean collectErrors) {
		this.collectErrors = collectErrors;
		return this;
	}

	public float getCondCommentIEVersion() {
		return condCommentIEVersion;
	}

	public LagartoDomBuilderConfig setCondCommentIEVersion(final float condCommentIEVersion) {
		this.condCommentIEVersion = condCommentIEVersion;
		return this;
	}

	public boolean isImpliedEndTags() {
		return impliedEndTags;
	}

	/**
	 * Enables implied end tags for certain tags.
	 * This flag reduces the performances a bit, so if you
	 * are dealing with 'straight' html that uses closes
	 * tags, consider switching this flag off.
	 */
	public LagartoDomBuilderConfig setImpliedEndTags(final boolean impliedEndTags) {
		this.impliedEndTags = impliedEndTags;
		return this;
	}

	public LagartoHtmlRenderer getHtmlRenderer() {
		return htmlRenderer;
	}

	/**
	 * Specifies new HTML rendered.
	 */
	public LagartoDomBuilderConfig setHtmlRenderer(final LagartoHtmlRenderer htmlRenderer) {
		this.htmlRenderer = Objects.requireNonNull(htmlRenderer);
		return this;
	}

	public LagartoParserConfig getParserConfig() {
		return parserConfig;
	}

	public LagartoDomBuilderConfig setParserConfig(final LagartoParserConfig parserConfig) {
		this.parserConfig = Objects.requireNonNull(parserConfig);
		return this;
	}

	public boolean isErrorLogEnabled() {
		return errorLogEnabled;
	}

	public LagartoDomBuilderConfig setErrorLogEnabled(final boolean errorLogEnabled) {
		this.errorLogEnabled = errorLogEnabled;
		return this;
	}

	public BiConsumer<Logger, String> getErrorLogConsumer() {
		return errorLogConsumer;
	}

	public LagartoDomBuilderConfig setErrorLogConsumer(final BiConsumer<Logger, String> errorLogConsumer) {
		this.errorLogConsumer = errorLogConsumer;
		return this;
	}
}
