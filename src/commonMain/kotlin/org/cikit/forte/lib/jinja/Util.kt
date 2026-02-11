package org.cikit.forte.lib.jinja

import org.cikit.forte.ForteBuilder
import org.cikit.forte.core.Context
import org.cikit.forte.parser.Declarations

// unimplemented filters
//
// * pprint(value: Any) → str
// ---
// * batch(value: 't.Iterable[V]', linecount: int, fill_with: 't.Optional[V]' = None) → 't.Iterator[t.List[V]]'
// * groupby(value: 't.Iterable[V]', attribute: str | int, default: Any | None = None, case_sensitive: bool = False) → 't.List[_GroupTuple]'
// * random(seq: 't.Sequence[V]') → 't.Union[V, Undefined]'
// * reverse(value: str | Iterable[V]) → str | Iterable[V]
// * slice(value: 't.Collection[V]', slices: int, fill_with: 't.Optional[V]' = None) → 't.Iterator[t.List[V]]'
// ---
// * abs(x, /)
// * round(value: float, precision: int = 0, method: 'te.Literal["common", "ceil", "floor"]' = 'common') → float
// * sum(iterable: 't.Iterable[V]', attribute: str | int | NoneType = None, start: V = 0) → V
// ---
// * capitalize(s: str) → str
// * center(value: str, width: int = 80) → str
// * indent(s: str, width: int | str = 4, first: bool = False, blank: bool = False) → str
// * title(s: str) → str
// * truncate(s: str, length: int = 255, killwords: bool = False, end: str = '...', leeway: int | None = None) → str
// * urlencode(value: str | Mapping[str, Any] | Iterable[Tuple[str, Any]]) → str
// * urlize(value: str, trim_url_limit: int | None = None, nofollow: bool = False, target: str | None = None, rel: str | None = None, extra_schemes: Iterable[str] | None = None) → str
// * wordcount(s: str) → int
// * wordwrap(s: str, width: int = 79, break_long_words: bool = True, wrapstring: str | None = None, break_on_hyphens: bool = True) → str

// unimplemented filters (require autoescape design)
//
// * escape (alias e?), forceescape, safe

// unimplemented filters (require external libs)
//
// * striptags(value: 't.Union[str, HasHTML]') → str
// * xmlattr(d: Mapping[str, Any], autospace: bool = True) → str
// * filesizeformat, format

// useless unimplemented tests
//
// * divisibleby(value: int, num: int) → bool

// unimplemented binary operators
//
// * `%`  (rem)    for string format
// * `**` (pow)    parsed right associative (use strict jinja compat to revert)

fun <R> Context.Builder<R>.defineJinjaExtensions(): Context.Builder<R> {
    defineMethod("attr", FilterAttr())

    defineMethod("escaped", IsEscapedTest())
    defineMethod("even", IsEvenTest(this))
    defineMethod("lower", IsLowerTest())
    defineMethod("none", IsNoneTest())
    defineMethod("odd", IsOddTest(this))
    defineMethod("sequence", IsSequenceTest(this))
    defineMethod("undefined", IsUndefinedTest(this))
    defineMethod("upper", IsUpperTest())

    return this
}

fun ForteBuilder.strictJinjaCompat(): ForteBuilder {
    var powIndex = 0
    var powDefault: Declarations.BinOp? = null
    while (powIndex < declarations.size) {
        val op = declarations[powIndex]
        if (op is Declarations.BinOp && op.name == "pow") {
            powDefault = op
            break
        }
        powIndex++
    }
    val powJinja = Declarations.BinOp(
        precedence = powDefault?.precedence ?: 80,
        aliases = powDefault?.aliases ?: setOf("**"),
        name = "pow",
        left = true,
        right = false,
        negate = null
    )
    if (powDefault == null) {
        declarations.add(powJinja)
    } else {
        declarations[powIndex] = powJinja
    }
    stringInterpolation = false
    return this
}
