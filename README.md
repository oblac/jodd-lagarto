# Jodd Lagarto

Java HTML parsers suite.

![GitHub release](https://img.shields.io/github/release/oblac/jodd-lagarto.svg)
![Maven Central](https://img.shields.io/maven-central/v/org.jodd/jodd-lagarto)
[![Build Status](https://img.shields.io/travis/oblac/jodd.svg)](https://travis-ci.org/oblac/jodd-lagarto)
[![codecov](https://codecov.io/gh/oblac/jodd-lagarto/branch/master/graph/badge.svg)](https://codecov.io/gh/oblac/jodd-lagarto)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/cb8b9fbd66a84265b4cf526f5887edd5)](https://www.codacy.com/gh/oblac/jodd-lagarto?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=oblac/jodd-lagarto&amp;utm_campaign=Badge_Grade)
[![Stack Overflow](https://img.shields.io/badge/stack%20overflow-lagarto-4183C4.svg)](https://stackoverflow.com/questions/tagged/jodd-lagarto)
[![BSD License](https://img.shields.io/badge/license-BSD--2--Clause-blue.svg)](https://github.com/oblac/jodd-lagarto/blob/master/LICENSE)

Lagarto Suite is the family of HTML/XML parsers written in Java. It consists of the following libraries:

+ `LagartoParser` is an all-purpose fast and versatile event-based HTML parser. You can use it to modify or analyze some markup content, allowing you to assemble custom complex transformations and code analysis tools quickly. It is performant and follows the rules of the official HTML specification.
+ `LagartoDom` builds a DOM tree in memory from the input. You can manipulate a tree more conveniently, with minor performance sacrifice.
+ `Jerry` is a "jQuery in Java" - you can use the familiar syntax of JavaScript library inside of Java to parse and manipulate HTML.
+ `CSSelly` - finally, the parser of CSS3 selectors.
