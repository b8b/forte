# StringConcatenation Tests

## Concatenation of empty strings returns an empty string

    {% assert eq("") %}{{ "" + "" }}{% endassert %}
    {% assert eq("") %}{{ "" * 100 }}{% endassert %}
    {% assert eq("") %}{{ "hello" * 0 }}{% endassert %}

## Get on empty string throws IndexOutOfBoundsException

    {% assert_fails as ex %}{{ ""[0] }}{% endassert %}
    {% assert contains("out of bounds") %}{{ ex|string }}{% endassert %}

## Append string to empty string

    {% set result = "" + "hello" %}
    {% assert_that result is defined %}
    {% assert_that result is eq("hello") %}
    {% assert_that "hello" is in(result) %}
    {% assert_true result|length == 5 %}
