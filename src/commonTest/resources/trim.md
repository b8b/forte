# trim tests

## test1

```
{% assert eq("line1\nline2\nline3") %}
line1
{# no trim #}line2
line3
{% endassert %}
```

## test2

```
{% assert eq("line1\nline2\nline3") %}
line1
   {#- trim before #}
line2
line3
{% endassert %}
```

## test3

```
{% assert eq("line1\nline2\nline3") %}
line1
{# trim after -#}
   line2
line3
{% endassert %}
```

## test4

```
{% assert eq("line1\nline2\nline3") %}
line1
   {#- trim both -#}
   {{ '\n' }}line2
line3
{% endassert %}
```

## test5

```
{% assert eq("line1\nline2\nline3") %}
line1
   {%- if false -%}
     {{ '\n' -}} not printed
   {%- endif %}
line2
   {%- if true -%}
     {{ '\n' -}} line3
   {%- endif %}
{% endassert %}
```
