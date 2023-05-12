# trim tests

## test1

```Twig
line1
line2
line3
~
line1
{# no trim #}line2
line3
```

## test2

```Twig
line1
line2
line3
~
line1
   {#- trim before #}
line2
line3
```

## test3

```Twig
line1
line2
line3
~
line1
{# trim after -#}
   line2
line3
```

## test4

```Twig
line1
line2
line3
~
line1
   {#- trim both -#}
   {{ '\n' }}line2
line3
```

## test5

```Twig
line1
line2
line3
~
line1
   {%- if false -%}
     {{ '\n' -}} not printed
   {%- endif %}
line2
   {%- if true -%}
     {{ '\n' -}} line3
   {%- endif %}
```
