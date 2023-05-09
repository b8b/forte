# Custom Tags

## debug

Command tag with single expression. No output.

```Twig
none
~
{% debug 1 + 1 %}none
```

## load_json

```Twig
[1,2,3]
~
{% load_json as x %}
[1, 2, 3]
{% endload %}{{ x|json }}
```
