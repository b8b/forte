# basic tests

## Raw Tag

```Twig
{# no comment #}
{% no command %}
{{ no emit }}
~
{%- raw -%}
{# no comment #}
{% no command %}
{{ no emit }}
{%- endraw -%}
```

## Range

```Twig
1, 2
~
{{ (1..2)|join(", ") }}
```

## IfElse

```Twig
OK
~
{% if true %}OK{% else %}BAD{% endif %}
```

## ForElse

```Twig
BAD
OKOKOK
OKa1OKb2OKa3
~
{% for x in [1, 2, 3] if x > 3 %}OK{% else %}BAD{% endfor %}
{% for x in [1, 2, 3] %}OK{% endfor %}
{% for x in [1, 2, 3] -%}
  OK
  {{- loop.cycle("a", "b") -}}
  {{- loop.index -}}
{% endfor %}
```

## ObjectLiteral

```Twig
{"a":11,"b":22}
~
{{ {a: 11, b: 22, }|json }}
```

## Arithmetic

```Twig
2
~
{{ 1 + 1 }}
```

## StringConcat
```Twig
hello yes true
~
hello {{ "yes #{true}" }}
```

## String Escapes
```Twig
"* \b \t \f \n \r \\ \" ' *"
"* \b \t \f \n \r \\ \" ' *"
~
{{ '* \b \t \f \n \r \\ " \' *'|json }}
{{ "* \b \t \f \n \r \\ \" ' *"|json }}
```

## Undefined
```Twig
x: -1
OK
true
~
x: {{ x|default(-1) }}
{% if x is not defined %}OK{% else %}BAD{% endif %}
{{ jinja is not defined }}
```

## Filter
```Twig
apply_int: 123
~
apply_int: {{ '123'|int }}
```

## Macro
```Twig
13
~
{% macro test1(a, b = 4 * 2) %}
{{- a + b -}}
{% endmacro %}{{ test1(a = 5) }}
```

## Nested Macro
```Twig
13
~
{% macro test1(a, b = 4 * 2) %}
{%- macro test2(x = a + b) -%}
{{- x -}}
{%- endmacro -%}
{{- test2() -}}
{% endmacro %}{{ test1(a = 5) }}
```

## Access vars from Macro
```Twig
3
~
{%- set a = 1 -%}
{%- set b = 2 -%}
{% macro test1() %}
{{- a + b -}}
{% endmacro %}{{ test1() }}
```

## Regex
```Twig
true,xxc,abc
~
{{- "abc"|matches_regex("[a-z]+") -}},
{{- "abc"|regex_replace("[A-B]", "x") -}},
{{- "abc"|regex_replace("[A-B]", "x", ignore_case = false) -}}
```

## Reject
```Twig
[1,3],[2]
~
{{- [1, 2, 3]|reject("==", 2)|json -}},
{{- [1, 2, 3]|select("==", 2)|json -}}
```

## ArrayAccess
```Twig
2,empty
~
{{- ([1, 2, 3, ])[1] -}},
{{- ([])[0]|default("empty") -}}
```

## Set Control
```Twig
hello there
~
{%- set name = "there" -%}
{%- set x -%}
hello {{ name }}
{%- endset -%}
{{- x -}}
```
