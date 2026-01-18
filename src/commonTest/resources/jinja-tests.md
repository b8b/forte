jinja-tests.boolean(value: Any) → bool

    Return true if the object is a boolean value.
    Changelog

jinja-tests.callable(obj, /)

    Return whether the object is callable (i.e., some kind of function).

    Note that classes are callable, as are instances of classes with a __call__() method.

jinja-tests.defined(value: Any) → bool

    Return true if the variable is defined:

    {% if variable is defined %}
        value of variable: {{ variable }}
    {% else %}
        variable is not defined
    {% endif %}

    See the default() filter for a simple way to set undefined variables.

jinja-tests.divisibleby(value: int, num: int) → bool

    Check if a variable is divisible by a number.

jinja-tests.eq(a, b, /)

    Same as a == b.

    Aliases:

        ==, equalto

jinja-tests.escaped(value: Any) → bool

    Check if the value is escaped.

jinja-tests.even(value: int) → bool

    Return true if the variable is even.

jinja-tests.false(value: Any) → bool

    Return true if the object is False.
    Changelog

jinja-tests.filter(value: str) → bool

    Check if a filter exists by name. Useful if a filter may be optionally available.

    {% if 'markdown' is filter %}
        {{ value | markdown }}
    {% else %}
        {{ value }}
    {% endif %}

    Changelog

jinja-tests.float(value: Any) → bool

    Return true if the object is a float.
    Changelog

jinja-tests.ge(a, b, /)

    Same as a >= b.

    Aliases:

        >=

jinja-tests.gt(a, b, /)

    Same as a > b.

    Aliases:

        >, greaterthan

jinja-tests.in(value: Any, seq: Container[Any]) → bool

    Check if value is in seq.
    Changelog

jinja-tests.integer(value: Any) → bool

    Return true if the object is an integer.
    Changelog

jinja-tests.iterable(value: Any) → bool

    Check if it’s possible to iterate over an object.

jinja-tests.le(a, b, /)

    Same as a <= b.

    Aliases:

        <=

jinja-tests.lower(value: str) → bool

    Return true if the variable is lowercased.

jinja-tests.lt(a, b, /)

    Same as a < b.

    Aliases:

        <, lessthan

jinja-tests.mapping(value: Any) → bool

    Return true if the object is a mapping (dict etc.).
    Changelog

jinja-tests.ne(a, b, /)

    Same as a != b.

    Aliases:

        !=

jinja-tests.none(value: Any) → bool

    Return true if the variable is none.

jinja-tests.number(value: Any) → bool

    Return true if the variable is a number.

jinja-tests.odd(value: int) → bool

    Return true if the variable is odd.

jinja-tests.sameas(value: Any, other: Any) → bool

    Check if an object points to the same memory address than another object:

    {% if foo.attribute is sameas false %}
        the foo attribute really is the `False` singleton
    {% endif %}

jinja-tests.sequence(value: Any) → bool

    Return true if the variable is a sequence. Sequences are variables that are iterable.

jinja-tests.string(value: Any) → bool

    Return true if the object is a string.

jinja-tests.test(value: str) → bool

    Check if a test exists by name. Useful if a test may be optionally available.

    {% if 'loud' is test %}
        {% if value is loud %}
            {{ value|upper }}
        {% else %}
            {{ value|lower }}
        {% endif %}
    {% else %}
        {{ value }}
    {% endif %}

    Changelog

jinja-tests.true(value: Any) → bool

    Return true if the object is True.
    Changelog

jinja-tests.undefined(value: Any) → bool

    Like defined() but the other way round.

jinja-tests.upper(value: str) → bool

    Return true if the variable is uppercased.
