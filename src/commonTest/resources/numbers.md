# Number Tests

## Negative Numbers

    {% assert eq("-4") %}{{ -(2+2) }}{% endassert %}
    {% assert eq("4") %}{{ - -(2+2) }}{% endassert %}
    {% assert eq("-4") %}{{ - - -(2+2) }}{% endassert %}

## Positive Numbers

    {% assert eq("2") %}{{ 1+1 }}{% endassert %}
    {% assert eq("2") %}{{ +(1+1) }}{% endassert %}
    {% assert eq("2") %}{{ + +(1+1) }}{% endassert %}
    {% assert eq("2") %}{{ + + +(1+1) }}{% endassert %}
    {% set ex %}{% assert_fails %}{{ +("1+1") }}{% endassert %}{% endset %}
    {% assert contains("operand") %}{{ ex }}{% endassert %}

## Binary and unary operators combined

    {% assert eq("8") %}{{ - (10 - 18) }}{% endassert %}
    {% assert eq("-28") %}{{ - 10 - 18 }}{% endassert %}
    {% assert eq("0") %}{{ - - 10 - 18 + 8 }}{% endassert %}
    {% assert eq("-10") %}{{ - + - + -10 }}{% endassert %}

## Integer division by zero fails

    {% set ex %}{% assert_fails %}{{ 1 / 0 }}{% endassert %}{% endset %}

## Float division by positive zero returns Infinity

    {% assert_that (1.0 / 0.0) is eq(Infinity) %}

## Float division by negative zero returns negative Infinity

    {% assert_that (1.0 / -0.0) is eq(-Infinity) %}

## Negative zero

    {% assert eq("-0.0") %}{{ -0.0 }}{% endassert %}
    {% assert eq("-0.0") %}{{ -0.0|tojson }}{% endassert %}

## Float encodes correctly to yaml

    {% assert eq("-0.0") %}{{ -0.0|toyaml }}{% endassert %}
    {% assert eq("-.inf") %}{{ -Infinity|toyaml }}{% endassert %}
    {% assert eq("+.inf") %}{{ +Infinity|toyaml }}{% endassert %}
    {% assert eq(".nan") %}{{ NaN|toyaml }}{% endassert %}

## NaN and Infinity

    {% assert eq("NaN") %}{{ NaN }}{% endassert %}
    {% assert eq("NaN") %}{{ +NaN }}{% endassert %}
    {% assert eq("NaN") %}{{ -NaN }}{% endassert %}
    {% assert eq("Infinity") %}{{ Infinity }}{% endassert %}
    {% assert eq("Infinity") %}{{ +Infinity }}{% endassert %}
    {% assert eq("-Infinity") %}{{ -Infinity }}{% endassert %}
    {% assert_false (1 ** Infinity) is number %}
    {% assert_that (1 ** Infinity) is sameas(NaN) %}

## Comparisons with NaN do fail

    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN < 5 }}{% endassert %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN <= 5 }}{% endassert  %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN > 5 }}{% endassert  %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN >= 5 }}{% endassert  %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN == 5 }}{% endassert  %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN < NaN }}{% endassert  %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN <= NaN }}{% endassert  %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN > NaN }}{% endassert  %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN >= NaN }}{% endassert  %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ NaN == NaN }}{% endassert  %}{% endassert %}

## Comparison between int and float

    {% assert_that 1 is gt(0.0) %}

    {% assert_that 1 is gt(0) %}
    {% assert_that 1.0 is gt(0) %}
    {% assert_that 1 is gt(0.0) %}
    {% assert_that 1.0 is gt(0.0) %}

    {% assert_that 0 is eq(0.0) %}
    {% assert_that 0.0 is eq(0) %}
    {% assert_that 0 is eq(0) %}
    {% assert_that 0.0 is eq(0.0) %}

    {% assert_that -1 is lt(0.0) %}
    {% assert_that -1.0 is lt(0) %}

    {% assert_that 2147483647 is gt(2147483646.0) %}
    {% assert_that 2147483647 is eq(2147483647.0) %}
    {% assert_that 2147483646.0 is lt(2147483647) %}

    {% assert_that -2147483648 is lt(-2147483647.0) %}
    {% assert_that -2147483648 is eq(-2147483648.0) %}
    {% assert_that -2147483647.0 is gt(-2147483648) %}

    {% assert_that 4294967295 is gt(4294967294.0) %}
    {% assert_that 4294967295 is eq(4294967295.0) %}

    {% assert_that 9007199254740992 is gt(9007199254740991.0) %}
    {% assert_that 9007199254740992 is eq(9007199254740992.0) %}
    {% assert_that 9007199254740991.0 is lt(9007199254740992) %}
    {% assert_that 9007199254740992.0 is lt(9007199254740993) %}

    {% assert_that -9007199254740992 is lt(-9007199254740991.0) %}
    {% assert_that -9007199254740992 is eq(-9007199254740992.0) %}

    {% assert_that 1 is gt(0.9999999999999999) %}
    {% assert_that 0 is gt(-0.9999999999999999) %}

    {% assert_that 1.0 is eq(1) %}
    {% assert_that 1 is eq(1.0) %}

    {% assert_that (1e10) is eq(10000000000) %}
    {% assert_that (1e15) is eq(1000000000000000) %}
    {% assert_that (1e16) is eq(10000000000000000) %}

    {% assert_that (0.1 + 0.2) is ne(0.3) %}

    {% assert_that (1.0 / 0.0) is eq(Infinity) %}
    {% assert_that (-1.0 / 0.0) is eq(-Infinity) %}
    {% assert_that (1 / 0.0) is eq(Infinity) %}

    {% assert_false 1 is gt(Infinity) %}
    {% assert_false 1.0 is gt(Infinity) %}
    {% assert_that 1 is gt(-Infinity) %}
    {% assert_that 1.0 is gt(-Infinity) %}

    {% assert_that 1 is lt(Infinity) %}
    {% assert_that 1.0 is lt(Infinity) %}
    {% assert_that (2 ** 8000) is lt(Infinity) %}
    {% assert_true 1 is not lt(-Infinity) %}
    {% assert_true 1.0 is not lt(-Infinity) %}
    {% assert_true (-2 ** 8000) is not lt(-Infinity) %}

    {% assert contains("not comparable") %}{% assert_fails %}{{ 1 is eq(NaN) }}{% endassert %}{% endassert %}
    {% assert contains("not comparable") %}{% assert_fails %}{{ 1.0 is eq(NaN) }}{% endassert %}{% endassert %}

    {% assert_that 0.0 is eq(-0.0) %}

    {% assert_that (1e-300) is gt(0) %}
    {% assert_that (1e-300) is gt(0.0) %}
    {% assert_that (1e-300) is gt(-0.0) %}

    {% assert_that (1e300) is gt(1) %}
    {% assert_that (1e300) is gt(1.0) %}
