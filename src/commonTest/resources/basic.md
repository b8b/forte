# basic tests

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
OKOKOK
~
{% for x in [1, 2, 3] %}OK{% else %}BAD{% endfor %}
```

## ObjectLiteral

```Twig
{"a":11,"b":22}
~
{{ {a: 11, b: 22}|json }}
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
x: null
BAD
true
~
x: {{ x }}
{% if x == true %}OK{% else %}BAD{% endif %}
{{ jinja is not defined }}
```

## Apply
```Twig
apply_int: 123
~
apply_int: {{ '123'|int }}
```

## Macro
```Twig
11
~
{% macro test1(a, b = 1) %}
{{- a + b -}}
{% endmacro %}{{ test1(5, 6) }}
```
