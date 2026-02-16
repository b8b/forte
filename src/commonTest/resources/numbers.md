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
    {% assert_fails as ex %}{{ +("1+1") }}{% endassert %}
    {% assert contains("operand") %}{{ ex }}{% endassert %}

## Binary and unary operators combined

    {% assert eq("8") %}{{ - (10 - 18) }}{% endassert %}
    {% assert eq("-28") %}{{ - 10 - 18 }}{% endassert %}
    {% assert eq("0") %}{{ - - 10 - 18 + 8 }}{% endassert %}
    {% assert eq("-10") %}{{ - + - + -10 }}{% endassert %}
