# Test Custom Tags

## debug

Command tag with single expression. No output.

    {% assert eq("none") %}{% debug 1 + 1 %}none{% endassert %}

## load_json

    {% assert eq("[1,2,3]") %}
        {%- load_json as x -%}
        [1, 2, 3]
        {%- endload -%}
        {{- x|json -}}
    {% endassert %}
