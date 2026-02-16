package org.cikit.forte.parser

sealed class Token(val first: Int, val last: Int) {
    private constructor(r: IntRange) : this(r.first, r.last)

    class Text(r: IntRange) : Token(r) {
        override fun toString(): String = "TText($first..$last)"
    }

    class BeginComment(r: IntRange) : Token(r) {
        override fun toString(): String = "TBeginComment($first..$last)"
    }

    class EndComment(r: IntRange) : Token(r) {
        override fun toString(): String = "TEndComment($first..$last)"
    }

    class BeginCommand(r: IntRange) : Token(r) {
        override fun toString(): String = "TBeginCommand($first..$last)"
    }

    class EndCommand(r: IntRange) : Token(r) {
        override fun toString(): String = "TEndCommand($first..$last)"
    }

    class BeginEmit(r: IntRange) : Token(r) {
        override fun toString(): String = "TBeginEmit($first..$last)"
    }

    class EndEmit(r: IntRange) : Token(r) {
        override fun toString(): String = "TEndEmit($first..$last)"
    }

    class Space(r: IntRange) : Token(r) {
        override fun toString(): String = "TSpace($first..$last)"
    }

    class Dot(r: IntRange) : Token(r) {
        override fun toString(): String = "TDot($first)"
    }

    class Comma(r: IntRange) : Token(r) {
        override fun toString(): String = "TComma($first)"
    }

    class Colon(r: IntRange) : Token(r) {
        override fun toString(): String = "TColon($first)"
    }

    class Assign(r: IntRange) : Token(r) {
        override fun toString(): String = "TAssign($first)"
    }

    class LPar(r: IntRange) : Token(r) {
        override fun toString(): String = "TLPar($first)"
    }

    class RPar(r: IntRange) : Token(r) {
        override fun toString(): String = "TRPar($first)"
    }

    class LBracket(r: IntRange) : Token(r) {
        override fun toString(): String = "TLBracket($first)"
    }

    class RBracket(r: IntRange) : Token(r) {
        override fun toString(): String = "TRBracket($first)"
    }

    class LBrace(r: IntRange) : Token(r) {
        override fun toString(): String = "TLBrace($first)"
    }

    class RBrace(r: IntRange) : Token(r) {
        override fun toString(): String = "TRBrace($first)"
    }

    class Number(r: IntRange) : Token(r) {
        override fun toString(): String = "TNumber($first..$last)"
    }

    class Identifier(r: IntRange) : Token(r) {
        override fun toString(): String = "TIdentifier($first..$last)"
    }

    class SingleQuote(r: IntRange) : Token(r) {
        override fun toString(): String = "TQ($first..$last)"
    }

    class DoubleQuote(r: IntRange) : Token(r) {
        override fun toString(): String = "TQQ($first..$last)"
    }

    class BeginInterpolation(r: IntRange) : Token(r) {
        override fun toString(): String = "TBeginInterpolation($first..$last)"
    }

    class Escape(r: IntRange) : Token(r) {
        override fun toString(): String = "TEscape($first..$last)"
    }

    class UnicodeEscape(r: IntRange) : Token(r) {
        override fun toString(): String = "TUEscape($first..$last)"
    }

    class InvalidEscape(r: IntRange) : Token(r) {
        override fun toString(): String = "TInvEscape($first)"
    }

    class Operator(r: IntRange) : Token(r) {
        override fun toString(): String = "TOp($first..$last)"
    }

    class Word(r: IntRange) : Token(r) {
        override fun toString(): String = "TWord($first..$last)"
    }

    class End(r: IntRange) : Token(r) {
        constructor(input: String) : this(input.length..input.length)
        override fun toString(): String = "TEnd($first)"
    }
}