
jinja-filters.abs(x, /)

    Return the absolute value of the argument.

jinja-filters.attr(obj: Any, name: str) → jinja2.runtime.Undefined | Any

    Get an attribute of an object. foo|attr("bar") works like foo.bar, but returns undefined instead of falling back to foo["bar"] if the attribute doesn’t exist.

    See Notes on subscriptions for more details.

jinja-filters.batch(value: 't.Iterable[V]', linecount: int, fill_with: 't.Optional[V]' = None) → 't.Iterator[t.List[V]]'

    A filter that batches items. It works pretty much like slice just the other way round. It returns a list of lists with the given number of items. If you provide a second parameter this is used to fill up missing items. See this example:

    <table>
    {%- for row in items|batch(3, '&nbsp;') %}
      <tr>
      {%- for column in row %}
        <td>{{ column }}</td>
      {%- endfor %}
      </tr>
    {%- endfor %}
    </table>

jinja-filters.capitalize(s: str) → str

    Capitalize a value. The first character will be uppercase, all others lowercase.

jinja-filters.center(value: str, width: int = 80) → str

    Centers the value in a field of a given width.

jinja-filters.default(value: V, default_value: V = '', boolean: bool = False) → V

    If the value is undefined it will return the passed default value, otherwise the value of the variable:

    {{ my_variable|default('my_variable is not defined') }}

    This will output the value of my_variable if the variable was defined, otherwise 'my_variable is not defined'. If you want to use default with variables that evaluate to false you have to set the second parameter to true:

    {{ ''|default('the string was empty', true) }}

    Changelog

    Changed in version 2.11: It’s now possible to configure the Environment with ChainableUndefined to make the default filter work on nested elements and attributes that may contain undefined values in the chain without getting an UndefinedError.

    Aliases:

        d

jinja-filters.dictsort(value: Mapping[K, V], case_sensitive: bool = False, by: 'te.Literal["key", "value"]' = 'key', reverse: bool = False) → List[Tuple[K, V]]

    Sort a dict and yield (key, value) pairs. Python dicts may not be in the order you want to display them in, so sort them first.

    {% for key, value in mydict|dictsort %}
        sort the dict by key, case insensitive

    {% for key, value in mydict|dictsort(reverse=true) %}
        sort the dict by key, case insensitive, reverse order

    {% for key, value in mydict|dictsort(true) %}
        sort the dict by key, case sensitive

    {% for key, value in mydict|dictsort(false, 'value') %}
        sort the dict by value, case insensitive

jinja-filters.escape(s: 't.Any', /) → 'Markup'

    Replace the characters &, <, >, ', and " in the string with HTML-safe sequences. Use this if you need to display text that might contain such characters in HTML.

    If the object has an __html__ method, it is called and the return value is assumed to already be safe for HTML.

    Parameters:

        s – An object to be converted to a string and escaped.
    Returns:

        A Markup string with the escaped text.
    Aliases:

        e

jinja-filters.filesizeformat(value: str | float | int, binary: bool = False) → str

    Format the value like a ‘human-readable’ file size (i.e. 13 kB, 4.1 MB, 102 Bytes, etc). Per default decimal prefixes are used (Mega, Giga, etc.), if the second parameter is set to True the binary prefixes are used (Mebi, Gibi).

jinja-filters.first(seq: 't.Iterable[V]') → 't.Union[V, Undefined]'

    Return the first item of a sequence.

jinja-filters.float(value: Any, default: float = 0.0) → float

    Convert the value into a floating point number. If the conversion doesn’t work it will return 0.0. You can override this default using the first parameter.

jinja-filters.forceescape(value: 't.Union[str, HasHTML]') → markupsafe.Markup

    Enforce HTML escaping. This will probably double escape variables.

jinja-filters.format(value: str, *args: Any, **kwargs: Any) → str

    Apply the given values to a printf-style format string, like string % values.

    {{ "%s, %s!"|format(greeting, name) }}
    Hello, World!

    In most cases it should be more convenient and efficient to use the % operator or str.format().

    {{ "%s, %s!" % (greeting, name) }}
    {{ "{}, {}!".format(greeting, name) }}

jinja-filters.groupby(value: 't.Iterable[V]', attribute: str | int, default: Any | None = None, case_sensitive: bool = False) → 't.List[_GroupTuple]'

    Group a sequence of objects by an attribute using Python’s itertools.groupby(). The attribute can use dot notation for nested access, like "address.city". Unlike Python’s groupby, the values are sorted first so only one group is returned for each unique value.

    For example, a list of User objects with a city attribute can be rendered in groups. In this example, grouper refers to the city value of the group.

    <ul>{% for city, items in users|groupby("city") %}
      <li>{{ city }}
        <ul>{% for user in items %}
          <li>{{ user.name }}
        {% endfor %}</ul>
      </li>
    {% endfor %}</ul>

    groupby yields namedtuples of (grouper, list), which can be used instead of the tuple unpacking above. grouper is the value of the attribute, and list is the items with that value.

    <ul>{% for group in users|groupby("city") %}
      <li>{{ group.grouper }}: {{ group.list|join(", ") }}
    {% endfor %}</ul>

    You can specify a default value to use if an object in the list does not have the given attribute.

    <ul>{% for city, items in users|groupby("city", default="NY") %}
      <li>{{ city }}: {{ items|map(attribute="name")|join(", ") }}</li>
    {% endfor %}</ul>

    Like the sort() filter, sorting and grouping is case-insensitive by default. The key for each group will have the case of the first item in that group of values. For example, if a list of users has cities ["CA", "NY", "ca"], the “CA” group will have two values. This can be disabled by passing case_sensitive=True.

    Changed in version 3.1: Added the case_sensitive parameter. Sorting and grouping is case-insensitive by default, matching other filters that do comparisons.
    Changelog

jinja-filters.indent(s: str, width: int | str = 4, first: bool = False, blank: bool = False) → str

    Return a copy of the string with each line indented by 4 spaces. The first line and blank lines are not indented by default.

    Parameters:

            width – Number of spaces, or a string, to indent by.

            first – Don’t skip indenting the first line.

            blank – Don’t skip indenting empty lines.

    Changelog

    Changed in version 3.0: width can be a string.

    Changed in version 2.10: Blank lines are not indented by default.

    Rename the indentfirst argument to first.

jinja-filters.int(value: Any, default: int = 0, base: int = 10) → int

    Convert the value into an integer. If the conversion doesn’t work it will return 0. You can override this default using the first parameter. You can also override the default base (10) in the second parameter, which handles input with prefixes such as 0b, 0o and 0x for bases 2, 8 and 16 respectively. The base is ignored for decimal numbers and non-string values.

jinja-filters.items(value: Mapping[K, V] | jinja2.runtime.Undefined) → Iterator[Tuple[K, V]]

    Return an iterator over the (key, value) items of a mapping.

    x|items is the same as x.items(), except if x is undefined an empty iterator is returned.

    This filter is useful if you expect the template to be rendered with an implementation of Jinja in another programming language that does not have a .items() method on its mapping type.

    <dl>
    {% for key, value in my_dict|items %}
        <dt>{{ key }}
        <dd>{{ value }}
    {% endfor %}
    </dl>

    Added in version 3.1.

jinja-filters.join(value: Iterable[Any], d: str = '', attribute: str | int | NoneType = None) → str

    Return a string which is the concatenation of the strings in the sequence. The separator between elements is an empty string per default, you can define it with the optional parameter:

    {{ [1, 2, 3]|join('|') }}
        -> 1|2|3

    {{ [1, 2, 3]|join }}
        -> 123

    It is also possible to join certain attributes of an object:

    {{ users|join(', ', attribute='username') }}

    Changelog

jinja-filters.last(seq: 't.Reversible[V]') → 't.Union[V, Undefined]'

    Return the last item of a sequence.

    Note: Does not work with generators. You may want to explicitly convert it to a list:

    {{ data | selectattr('name', '==', 'Jinja') | list | last }}

jinja-filters.length(obj, /)

    Return the number of items in a container.

    Aliases:

        count

jinja-filters.list(value: 't.Iterable[V]') → 't.List[V]'

    Convert the value into a list. If it was a string the returned list will be a list of characters.

jinja-filters.lower(s: str) → str

    Convert a value to lowercase.

jinja-filters.map(value: Iterable[Any], *args: Any, **kwargs: Any) → Iterable[Any]

    Applies a filter on a sequence of objects or looks up an attribute. This is useful when dealing with lists of objects but you are really only interested in a certain value of it.

    The basic usage is mapping on an attribute. Imagine you have a list of users but you are only interested in a list of usernames:

    Users on this page: {{ users|map(attribute='username')|join(', ') }}

    You can specify a default value to use if an object in the list does not have the given attribute.

    {{ users|map(attribute="username", default="Anonymous")|join(", ") }}

    Alternatively you can let it invoke a filter by passing the name of the filter and the arguments afterwards. A good example would be applying a text conversion filter on a sequence:

    Users on this page: {{ titles|map('lower')|join(', ') }}

    Similar to a generator comprehension such as:

    (u.username for u in users)
    (getattr(u, "username", "Anonymous") for u in users)
    (do_lower(x) for x in titles)

    Changelog

jinja-filters.max(value: 't.Iterable[V]', case_sensitive: bool = False, attribute: str | int | NoneType = None) → 't.Union[V, Undefined]'

    Return the largest item from the sequence.

    {{ [1, 2, 3]|max }}
        -> 3

    Parameters:

            case_sensitive – Treat upper and lower case strings as distinct.

            attribute – Get the object with the max value of this attribute.

jinja-filters.min(value: 't.Iterable[V]', case_sensitive: bool = False, attribute: str | int | NoneType = None) → 't.Union[V, Undefined]'

    Return the smallest item from the sequence.

    {{ [1, 2, 3]|min }}
        -> 1

    Parameters:

            case_sensitive – Treat upper and lower case strings as distinct.

            attribute – Get the object with the min value of this attribute.

jinja-filters.pprint(value: Any) → str

    Pretty print a variable. Useful for debugging.

jinja-filters.random(seq: 't.Sequence[V]') → 't.Union[V, Undefined]'

    Return a random item from the sequence.

jinja-filters.reject(value: 't.Iterable[V]', *args: Any, **kwargs: Any) → 't.Iterator[V]'

    Filters a sequence of objects by applying a test to each object, and rejecting the objects with the test succeeding.

    If no test is specified, each object will be evaluated as a boolean.

    Example usage:

    {{ numbers|reject("odd") }}

    Similar to a generator comprehension such as:

    (n for n in numbers if not test_odd(n))

    Changelog

jinja-filters.rejectattr(value: 't.Iterable[V]', *args: Any, **kwargs: Any) → 't.Iterator[V]'

    Filters a sequence of objects by applying a test to the specified attribute of each object, and rejecting the objects with the test succeeding.

    If no test is specified, the attribute’s value will be evaluated as a boolean.

    {{ users|rejectattr("is_active") }}
    {{ users|rejectattr("email", "none") }}

    Similar to a generator comprehension such as:

    (user for user in users if not user.is_active)
    (user for user in users if not test_none(user.email))

    Changelog

jinja-filters.replace(s: str, old: str, new: str, count: int | None = None) → str

    Return a copy of the value with all occurrences of a substring replaced with a new one. The first argument is the substring that should be replaced, the second is the replacement string. If the optional third argument count is given, only the first count occurrences are replaced:

    {{ "Hello World"|replace("Hello", "Goodbye") }}
        -> Goodbye World

    {{ "aaaaargh"|replace("a", "d'oh, ", 2) }}
        -> d'oh, d'oh, aaargh

jinja-filters.reverse(value: str | Iterable[V]) → str | Iterable[V]

    Reverse the object or return an iterator that iterates over it the other way round.

jinja-filters.round(value: float, precision: int = 0, method: 'te.Literal["common", "ceil", "floor"]' = 'common') → float

    Round the number to a given precision. The first parameter specifies the precision (default is 0), the second the rounding method:

        'common' rounds either up or down

        'ceil' always rounds up

        'floor' always rounds down

    If you don’t specify a method 'common' is used.

    {{ 42.55|round }}
        -> 43.0
    {{ 42.55|round(1, 'floor') }}
        -> 42.5

    Note that even if rounded to 0 precision, a float is returned. If you need a real integer, pipe it through int:

    {{ 42.55|round|int }}
        -> 43

jinja-filters.safe(value: str) → markupsafe.Markup

    Mark the value as safe which means that in an environment with automatic escaping enabled this variable will not be escaped.

jinja-filters.select(value: 't.Iterable[V]', *args: Any, **kwargs: Any) → 't.Iterator[V]'

    Filters a sequence of objects by applying a test to each object, and only selecting the objects with the test succeeding.

    If no test is specified, each object will be evaluated as a boolean.

    Example usage:

    {{ numbers|select("odd") }}
    {{ numbers|select("odd") }}
    {{ numbers|select("divisibleby", 3) }}
    {{ numbers|select("lessthan", 42) }}
    {{ strings|select("equalto", "mystring") }}

    Similar to a generator comprehension such as:

    (n for n in numbers if test_odd(n))
    (n for n in numbers if test_divisibleby(n, 3))

    Changelog

jinja-filters.selectattr(value: 't.Iterable[V]', *args: Any, **kwargs: Any) → 't.Iterator[V]'

    Filters a sequence of objects by applying a test to the specified attribute of each object, and only selecting the objects with the test succeeding.

    If no test is specified, the attribute’s value will be evaluated as a boolean.

    Example usage:

    {{ users|selectattr("is_active") }}
    {{ users|selectattr("email", "none") }}

    Similar to a generator comprehension such as:

    (user for user in users if user.is_active)
    (user for user in users if test_none(user.email))

    Changelog

jinja-filters.slice(value: 't.Collection[V]', slices: int, fill_with: 't.Optional[V]' = None) → 't.Iterator[t.List[V]]'

    Slice an iterator and return a list of lists containing those items. Useful if you want to create a div containing three ul tags that represent columns:

    <div class="columnwrapper">
      {%- for column in items|slice(3) %}
        <ul class="column-{{ loop.index }}">
        {%- for item in column %}
          <li>{{ item }}</li>
        {%- endfor %}
        </ul>
      {%- endfor %}
    </div>

    If you pass it a second argument it’s used to fill missing values on the last iteration.

jinja-filters.sort(value: 't.Iterable[V]', reverse: bool = False, case_sensitive: bool = False, attribute: str | int | NoneType = None) → 't.List[V]'

    Sort an iterable using Python’s sorted().

    {% for city in cities|sort %}
        ...
    {% endfor %}

    Parameters:

            reverse – Sort descending instead of ascending.

            case_sensitive – When sorting strings, sort upper and lower case separately.

            attribute – When sorting objects or dicts, an attribute or key to sort by. Can use dot notation like "address.city". Can be a list of attributes like "age,name".

    The sort is stable, it does not change the relative order of elements that compare equal. This makes it is possible to chain sorts on different attributes and ordering.

    {% for user in users|sort(attribute="name")
        |sort(reverse=true, attribute="age") %}
        ...
    {% endfor %}

    As a shortcut to chaining when the direction is the same for all attributes, pass a comma separate list of attributes.

    {% for user in users|sort(attribute="age,name") %}
        ...
    {% endfor %}

    Changelog

jinja-filters.string(s: 't.Any', /) → 'str'

    Convert an object to a string if it isn’t already. This preserves a Markup string rather than converting it back to a basic string, so it will still be marked as safe and won’t be escaped again.

value = escape("<User 1>")

value
Markup('&lt;User 1&gt;')

escape(str(value))
Markup('&amp;lt;User 1&amp;gt;')

    escape(soft_str(value))
    Markup('&lt;User 1&gt;')

jinja-filters.striptags(value: 't.Union[str, HasHTML]') → str

    Strip SGML/XML tags and replace adjacent whitespace by one space.

jinja-filters.sum(iterable: 't.Iterable[V]', attribute: str | int | NoneType = None, start: V = 0) → V

    Returns the sum of a sequence of numbers plus the value of parameter ‘start’ (which defaults to 0). When the sequence is empty it returns start.

    It is also possible to sum up only certain attributes:

    Total: {{ items|sum(attribute='price') }}

    Changelog

jinja-filters.title(s: str) → str

    Return a titlecased version of the value. I.e. words will start with uppercase letters, all remaining characters are lowercase.

jinja-filters.tojson(value: Any, indent: int | None = None) → markupsafe.Markup

    Serialize an object to a string of JSON, and mark it safe to render in HTML. This filter is only for use in HTML documents.

    The returned string is safe to render in HTML documents and <script> tags. The exception is in HTML attributes that are double quoted; either use single quotes or the |forceescape filter.

    Parameters:

            value – The object to serialize to JSON.

            indent – The indent parameter passed to dumps, for pretty-printing the value.

    Changelog

jinja-filters.trim(value: str, chars: str | None = None) → str

    Strip leading and trailing characters, by default whitespace.

jinja-filters.truncate(s: str, length: int = 255, killwords: bool = False, end: str = '...', leeway: int | None = None) → str

    Return a truncated copy of the string. The length is specified with the first parameter which defaults to 255. If the second parameter is true the filter will cut the text at length. Otherwise it will discard the last word. If the text was in fact truncated it will append an ellipsis sign ("..."). If you want a different ellipsis sign than "..." you can specify it using the third parameter. Strings that only exceed the length by the tolerance margin given in the fourth parameter will not be truncated.

    {{ "foo bar baz qux"|truncate(9) }}
        -> "foo..."
    {{ "foo bar baz qux"|truncate(9, True) }}
        -> "foo ba..."
    {{ "foo bar baz qux"|truncate(11) }}
        -> "foo bar baz qux"
    {{ "foo bar baz qux"|truncate(11, False, '...', 0) }}
        -> "foo bar..."

    The default leeway on newer Jinja versions is 5 and was 0 before but can be reconfigured globally.

jinja-filters.unique(value: 't.Iterable[V]', case_sensitive: bool = False, attribute: str | int | NoneType = None) → 't.Iterator[V]'

    Returns a list of unique items from the given iterable.

    {{ ['foo', 'bar', 'foobar', 'FooBar']|unique|list }}
        -> ['foo', 'bar', 'foobar']

    The unique items are yielded in the same order as their first occurrence in the iterable passed to the filter.

    Parameters:

            case_sensitive – Treat upper and lower case strings as distinct.

            attribute – Filter objects with unique values for this attribute.

jinja-filters.upper(s: str) → str

    Convert a value to uppercase.

jinja-filters.urlencode(value: str | Mapping[str, Any] | Iterable[Tuple[str, Any]]) → str

    Quote data for use in a URL path or query using UTF-8.

    Basic wrapper around urllib.parse.quote() when given a string, or urllib.parse.urlencode() for a dict or iterable.

    Parameters:

        value – Data to quote. A string will be quoted directly. A dict or iterable of (key, value) pairs will be joined as a query string.

    When given a string, “/” is not quoted. HTTP servers treat “/” and “%2F” equivalently in paths. If you need quoted slashes, use the |replace("/", "%2F") filter.
    Changelog

jinja-filters.urlize(value: str, trim_url_limit: int | None = None, nofollow: bool = False, target: str | None = None, rel: str | None = None, extra_schemes: Iterable[str] | None = None) → str

    Convert URLs in text into clickable links.

    This may not recognize links in some situations. Usually, a more comprehensive formatter, such as a Markdown library, is a better choice.

    Works on http://, https://, www., mailto:, and email addresses. Links with trailing punctuation (periods, commas, closing parentheses) and leading punctuation (opening parentheses) are recognized excluding the punctuation. Email addresses that include header fields are not recognized (for example, mailto:address@example.com?cc=copy@example.com).

    Parameters:

            value – Original text containing URLs to link.

            trim_url_limit – Shorten displayed URL values to this length.

            nofollow – Add the rel=nofollow attribute to links.

            target – Add the target attribute to links.

            rel – Add the rel attribute to links.

            extra_schemes – Recognize URLs that start with these schemes in addition to the default behavior. Defaults to env.policies["urlize.extra_schemes"], which defaults to no extra schemes.

    Changelog

    Changed in version 3.0: The extra_schemes parameter was added.

    Changed in version 3.0: Generate https:// links for URLs without a scheme.

    Changed in version 3.0: The parsing rules were updated. Recognize email addresses with or without the mailto: scheme. Validate IP addresses. Ignore parentheses and brackets in more cases.

    Changed in version 2.8: The target parameter was added.

jinja-filters.wordcount(s: str) → int

    Count the words in that string.

jinja-filters.wordwrap(s: str, width: int = 79, break_long_words: bool = True, wrapstring: str | None = None, break_on_hyphens: bool = True) → str

    Wrap a string to the given width. Existing newlines are treated as paragraphs to be wrapped separately.

    Parameters:

            s – Original text to wrap.

            width – Maximum length of wrapped lines.

            break_long_words – If a word is longer than width, break it across lines.

            break_on_hyphens – If a word contains hyphens, it may be split across lines.

            wrapstring – String to join each wrapped line. Defaults to Environment.newline_sequence.

    Changelog

    Changed in version 2.11: Existing newlines are treated as paragraphs wrapped separately.

    Changed in version 2.11: Added the break_on_hyphens parameter.

    Changed in version 2.7: Added the wrapstring parameter.

jinja-filters.xmlattr(d: Mapping[str, Any], autospace: bool = True) → str

    Create an SGML/XML attribute string based on the items in a dict.

    Values that are neither none nor undefined are automatically escaped, safely allowing untrusted user input.

    User input should not be used as keys to this filter. If any key contains a space, / solidus, > greater-than sign, or = equals sign, this fails with a ValueError. Regardless of this, user input should never be used as keys to this filter, or must be separately validated first.

    <ul{{ {'class': 'my_list', 'missing': none,
            'id': 'list-%d'|format(variable)}|xmlattr }}>
    ...
    </ul>

    Results in something like this:

    <ul class="my_list" id="list-42">
    ...
    </ul>

    As you can see it automatically prepends a space in front of the item if the filter returned something unless the second parameter is false.

    Changed in version 3.1.4: Keys with / solidus, > greater-than sign, or = equals sign are not allowed.

    Changed in version 3.1.3: Keys with spaces are not allowed.
