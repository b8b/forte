# basic tests

## Raw Tag

```twig
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

```twig
1, 2
~
{{ range(1, 3)|join(", ") }}
```

## IfElse

```twig
OK
~
{% if true %}OK{% else %}BAD{% endif %}
```

## ForElse

```twig
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

```twig
{"a":11,"b":22}
~
{{ {a: 11, b: 22, }|json }}
```

## Arithmetic

```twig
2
1208925819614629174706177
1152921504606846976
2147483648
~
{{ 1 + 1 }}
{{ 1208925819614629174706176 + 1 }}
{{ 2 ** 60 }}
{{ 2147483647 + 1 }}
```

## StringConcat
```twig
hello yes true
hello yes true
~
hello {{ "yes #{true}" }}
hello {{ "yes " ~ true }}
```

## String Escapes
```twig
"* \b \t \f \n \r \\ \" ' *"
"* \b \t \f \n \r \\ \" ' *"
~
{{ '* \b \t \f \n \r \\ " \' *'|json }}
{{ "* \b \t \f \n \r \\ \" ' *"|json }}
```

## Undefined
```twig
x: -1
OK
true
~
x: {{ x|default(-1) }}
{% if x is not defined %}OK{% else %}BAD{% endif %}
{{ jinja is not defined }}
```

## Filter
```twig
apply_int: 123
~
apply_int: {{ '123'|int }}
```

## Macro
```twig
13
~
{% macro test1(a, b = 4 * 2) %}
{{- a + b -}}
{% endmacro %}{{ test1(a = 5) }}
```

## Nested Macro
```twig
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
```twig
3
~
{%- set a = 1 -%}
{%- set b = 2 -%}
{% macro test1() %}
{{- a + b -}}
{% endmacro %}{{ test1() }}
```

## Regex
```twig
true,xxc,abc
~
{{- "abc"|matches_regex("[a-z]+") -}},
{{- "abc"|regex_replace("[A-B]", "x") -}},
{{- "abc"|regex_replace("[A-B]", "x", ignore_case = false) -}}
```

## Reject
```twig
[1,3],[2]
~
{{- [1, 2, 3]|reject("==", 2)|json -}},
{{- [1, 2, 3]|select("==", 2)|json -}}
```

## ArrayAccess
```twig
2,empty
~
{{- ([1, 2, 3, ])[1] -}},
{{- ([])[0]|default("empty") -}}
```

## Set Control
```twig
hello there
~
{%- set name = "there" -%}
{%- set x -%}
hello {{ name }}
{%- endset -%}
{{- x -}}
```

## Selectattr
```twig
[{"a":false}]
[{"a":false}]
[]
[{"b":2}]
[{"a":false}]
[{"a":false},{"b":2}]
~
{%- set input = [{"a": false}, {"b": 2}] -%}
{{- input|selectattr("a")|json }}
{{ input|selectattr("a", "eq", false)|json }}
{{ input|selectattr("x", "eq", 1)|json }}
{{ input|rejectattr("a")|json }}
{{ input|rejectattr("b")|json }}
{{ input|rejectattr("c", "eq", 15)|json }}
```

## Map
```twig
["10","2","3"]
[2,3,10]
~
{{ [2, "3", 10]|map("string")|sort|json }}
{{ [10, 10.0, 2, 3]|unique|json }}
```

## In
```twig
true
~
{{ 1.0 in [1, 2, 3] }}
```

## Dictsort
```twig
a=2,z=1,
~
{% for k, v in {z: 1, a: 2}|dictsort -%}
{{ k }}={{ v }},
{%- endfor %}
```

## Generic sort
```twig
["BWYFiOpx","RcNbwutO","xCIexbxF"]
~
{{ ["RcNbwutO", "xCIexbxF", "BWYFiOpx"]|map("base64_decode")|sort|map("base64_encode")|json }}
```

## Iterable compare
```twig
true
~
{{ [1, 2, 3] < [1, 2, 4] }}
```

## Filter call
```twig
BLAH
~
{% filter upper -%}
blah
{%- endfilter %}
```

## Set multiple variables
```twig
1
~
{% set x, y = [1, 2] %}{{ x }}
```

## Eq
```twig
false,true,true,false
false,true,true,false
~
{% set a = null %}{% set b = "some" -%}
{{ a != null }},{{ a == null }},{{ b != null }},{{ b == null }}
{{ a is ne(null) }},{{ a is eq(null) }},{{ b is ne(null) }},{{ b is eq(null) }}
```

## If expression
```twig
1
4
~
{{ 1 if true else 2 }}
{{ 3 if false else 4 }}
```

## MinMax
```twig
1
3
~
{{ [1,2,3]|min }}
{{ [1,2,3]|max }}
```

## Yaml
```twig
"a": 1
~
{{ {a: 1}|yaml }}
```
