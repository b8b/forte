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

## NaN and Infinity

    {% assert eq("NaN") %}{{ NaN }}{% endassert %}
    {% assert eq("NaN") %}{{ +NaN }}{% endassert %}
    {% assert eq("NaN") %}{{ -NaN }}{% endassert %}
    {% assert eq("Infinity") %}{{ Infinity }}{% endassert %}
    {% assert eq("Infinity") %}{{ +Infinity }}{% endassert %}
    {% assert eq("-Infinity") %}{{ -Infinity }}{% endassert %}
