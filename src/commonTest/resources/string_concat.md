# StringConcatenation Tests

## Concatenation of empty strings returns an empty string

    {% assert eq("") %}{{ "" + "" }}{% endassert %}
    {% assert eq("") %}{{ "" * 100 }}{% endassert %}
    {% assert eq("") %}{{ "hello" * 0 }}{% endassert %}

## Get on empty string throws IndexOutOfBoundsException

    {% set ex %}{% assert_fails %}{{ ""[0]|int }}{% endassert %}{% endset %}
    {% if "index 0 out of bounds" not in ex %}
        {% assert_that ex is eq("str object has no element 0") %}
    {% endif %}

## Append string to empty string

    {% set result = "" + "hello" %}
    {% assert_that result is defined %}
    {% assert_that result is eq("hello") %}
    {% assert_that "hello" is in(result) %}
    {% assert_true result|length == 5 %}
