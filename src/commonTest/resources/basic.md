# Basic Tests

## Raw Tag

    {% assert eq("{# no comment #}") %}{% raw %}{# no comment #}{% endraw %}{% endassert %}
    {% assert eq("{% no command %}") %}{% raw %}{% no command %}{% endraw %}{% endassert %}
    {% assert eq("{{ no emit }}") %}{% raw %}{{ no emit }}{% endraw %}{% endassert %}

## Range

    {% assert eq("1, 2") %}{{ range(1, 3)|join(", ") }}{% endassert %}

## IfElse

    {% assert eq("OK") %}{% if true %}OK{% else %}BAD{% endif %}{% endassert %}

## ForElse

    {% assert eq("BAD") %}{% for x in [1, 2, 3] if x > 3 %}OK{% else %}BAD{% endfor %}{% endassert %}
    {% assert eq("OKOKOK") %}{% for x in [1, 2, 3] %}OK{% endfor %}{% endassert %}
    {% assert eq("OKa1OKb2OKa3") %}
        {%- for x in [1, 2, 3] -%}
          OK
          {{- loop.cycle("a", "b") -}}
          {{- loop.index -}}
        {%- endfor -%}
    {% endassert %}

## ObjectLiteral

    {% set d = {"a": 11, "b": 22, } %}
    {% assert_that d|length is eq(2) %}
    {% assert_that d["a"] is eq(11) %}
    {% assert_that d["b"] is eq(22) %}
    {% set k1 = "name" %}
    {% set k2 = "age" %}
    {% set d = {k1: "jan", k2: 22} %}
    {% assert_that d|length is eq(2) %}
    {% assert_that d["name"] is eq("jan") %}
    {% assert_that d["age"] is eq(22) %}

## Arithmetic

    {% assert eq("2") %}{{ 1 + 1 }}{% endassert %}
    {% assert eq("1208925819614629174706177") %}{{ 1208925819614629174706176 + 1 }}{% endassert %}
    {% assert eq("1152921504606846976") %}{{ 2 ** 60 }}{% endassert %}
    {% assert eq("2147483648") %}{{ 2147483647 + 1 }}{% endassert %}


## StringConcat

    {% assert eq("hello yes True") %}hello {{ "yes " ~ true }}{% endassert %}
    {% assert eq("hello yes True") %}hello {{ "yes #{true}" }}{% endassert %}

## String Escapes

    {% set expect %}{% raw %}"* \b \t \f \n \r \\ \" ' *"{% endraw %}{% endset %}
    {% assert eq(expect) %}{{ '* \b \t \f \n \r \\ " \' *'|tojson|replace("\\u0027", "'") }}{% endassert %}
    {% assert eq(expect) %}{{ "* \b \t \f \n \r \\ \" ' *"|tojson|replace("\\u0027", "'") }}{% endassert %}

## Undefined

    {% assert eq("x: -1") %}x: {{ x|default(-1) }}{% endassert %}
    {% assert eq("OK") %}{% if x is not defined %}OK{% else %}BAD{% endif %}{% endassert %}
    {% assert eq("True") %}{{ jinja is not defined }}{% endassert %}

## Filter

    {% assert eq("apply_int: 123") %}apply_int: {{ '123'|int }}{% endassert %}

## Macro

    {% assert eq("13") %}
        {%- macro test1(a, b = 4 * 2) -%}
        {{- a + b -}}
        {%- endmacro -%}
        {{- test1(a = 5) -}}
    {% endassert %}

## Nested Macro

    {% assert eq("13") %}
        {%- macro test1(a, b = 4 * 2) -%}
            {%- macro test2(x = a + b) -%}
            {{- x -}}
            {%- endmacro -%}
            {{- test2() -}}
        {%- endmacro -%}
        {{- test1(a = 5) -}}
    {% endassert %}

## Access vars from Macro

    {% assert eq("3") %}
        {%- set a = 1 -%}
        {%- set b = 2 -%}
        {%- macro test1() -%}
        {{- a + b -}}
        {%- endmacro -%}
        {{- test1() -}}
    {% endassert %}

## Regex

    {% assert eq("True") %}{{- "abc"|matches_regex("[a-z]+") -}}{% endassert %}
    {% assert eq("xxc") %}{{- "abc"|regex_replace("[A-B]", "x") -}}{% endassert %}
    {% assert eq("abc") %}{{- "abc"|regex_replace("[A-B]", "x", ignore_case = false) -}}{% endassert %}

## Reject

    {% assert eq("1|3") %}{{- [1, 2, 3]|reject("==", 2)|list|join("|") -}}{% endassert %}
    {% assert eq("2") %}{{- [1, 2, 3]|select("==", 2)|list|join("|") -}}{% endassert %}

## ArrayAccess

    {% assert eq("2") %}{{- ([1, 2, 3, ])[1] -}}{% endassert %}
    {% assert eq("empty") %}{{- ([])[0]|default("empty") -}}{% endassert %}

## Set Control

    {% assert eq("hello there") %}
        {%- set name = "there" -%}
        {%- set x -%}
        hello {{ name }}
        {%- endset -%}
        {{- x -}}
    {% endassert %}

## Selectattr

    {% set input = [{"a": false}, {"b": 2}] %}
    {% assert_that input|selectattr("a")|list is eq([]) %}
    {% assert_that input|selectattr("a", "eq", false)|list is eq([{"a": false}]) %}
    {% assert_that input|selectattr("x", "eq", 1)|list is eq([]) %}
    {% assert_that input|rejectattr("a")|list is eq([{"a": false}, {"b": 2}]) %}
    {% assert_that input|rejectattr("b")|list is eq([{"a": false}, {"b": 2}]) %}
    {% assert_that input|rejectattr("c", "eq", 15)|list is eq([{"a": false}, {"b": 2}]) %}

## Map

    {% assert_that [2, "3", 10]|map("string")|sort is eq(["10", "2", "3"]) %}
    {% assert_that [10, 10.0, 2, 3]|unique|sort is eq([2, 3, 10]) %}


## In

    {% assert eq("True") %}{{ 1.0 in [1, 2, 3] }}{% endassert %}


## Dictsort

    {% assert eq("a=2,z=1,") %}
        {%- for k, v in {"z": 1, "a": 2}|dictsort -%}
        {{- k }}={{ v -}},
        {%- endfor -%}
    {% endassert %}

## Generic sort

    {% assert eq('["BWYFiOpx","RcNbwutO","xCIexbxF"]') %}
        {{- ["RcNbwutO", "xCIexbxF", "BWYFiOpx"]
           |map("base64_decode")
           |sort
           |map("base64_encode")
           |list
           |tojson 
        -}}
    {% endassert %}

## Iterable compare

    {% assert eq("True") %}{{ [1, 2, 3] < [1, 2, 4] }}{% endassert %}

## Filter call

    {% assert eq("BLAH") %}
        {%- filter upper -%}
        blah
        {%- endfilter -%}
    {% endassert %}

    {% assert eq("Goodbye World") %}
        {%- filter replace("Hello", "Goodbye") -%}
        Hello World
        {%- endfilter -%}
    {% endassert %}

## Set multiple variables

    {% assert eq("1") %}{% set x, y = [1, 2] %}{{ x }}{% endassert %}

## Eq

    {% set a = None %}{% set b = "some" %}
    {% assert eq("False,True,True,False") %}{{ a != None }},{{ a == None }},{{ b != None }},{{ b == None }}{% endassert %}
    {% assert_that (a is ne(None)) is false %}
    {% assert_that (a is eq(None)) is true %}
    {% assert_that (b is ne(None)) is true %}
    {% assert_that (b is eq(None)) is false %}

## If expression

    {% assert eq("1") %}{{ 1 if true else 2 }}{% endassert %}
    {% assert eq("4") %}{{ 3 if false else 4 }}{% endassert %}

## MinMax

    {% assert eq("1") %}{{ [1,2,3]|min }}{% endassert %}
    {% assert eq("3") %}{{ [1,2,3]|max }}{% endassert %}

## Yaml

    {% assert eq('"a": 1') %}{{ {"a": 1}|yaml }}{% endassert %}

## Dict

    {% assert_that dict(a=1, b=2)|dictsort|map("list")|list is eq([["a", 1], ["b", 2]]) %}
    {% assert_that dict({}, a=1, b=2)|dictsort|map("list")|list is eq([["a", 1], ["b", 2]]) %}
    {% assert_that dict({"a": 5}, a=1, b=2)|dictsort|map("list")|list is eq([["a", 1], ["b", 2]]) %}
    {% assert_that dict([[1, 2], [3, 4]])|dictsort|map("list")|list is eq([[1, 2], [3, 4]]) %}
    {% assert_that dict()|dictsort|map("list")|list is eq([]) %}
    {% set x = dict({"a": 1, "b": 2}|items|list + {"b": 3}|items|list)|dictsort|map("list")|list %}
    {% assert_that x is eq([["a", 1], ["b", 3]]) %}
