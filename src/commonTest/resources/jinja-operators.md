# Math

Jinja allows you to calculate with values. This is rarely useful in templates but exists for completeness’ sake. The following operators are supported:

## +

Adds two objects together. Usually the objects are numbers, but if both are strings or lists, you can concatenate them this way. This, however, is not the preferred way to concatenate strings! For string concatenation, have a look-see at the ~ operator. {{ 1 + 1 }} is 2.

## -

Subtract the second number from the first one. {{ 3 - 2 }} is 1.

## /

Divide two numbers. The return value will be a floating point number. {{ 1 / 2 }} is {{ 0.5 }}.

## //

Divide two numbers and return the truncated integer result. {{ 20 // 7 }} is 2.

## %

Calculate the remainder of an integer division. {{ 11 % 7 }} is 4.
(also used for string format)

## *

Multiply the left operand with the right one. {{ 2 * 2 }} would return 4. This can also be used to repeat a string multiple times. {{ '=' * 80 }} would print a bar of 80 equal signs.

## **

Raise the left operand to the power of the right operand. {{ 2\*\*3 }} would return 8.

Unlike Python, chained pow is evaluated left to right. {{ 3\*\*3\*\*3 }} is evaluated as (3\*\*3)\*\*3 in Jinja, but would be evaluated as 3\*\*(3\*\*3) in Python. Use parentheses in Jinja to be explicit about what order you want. It is usually preferable to do extended math in Python and pass the results to render rather than doing it in the template.

This behavior may be changed in the future to match Python, if it’s possible to introduce an upgrade path.

(simply drop this in jinja compat mode)

# Comparisons

## ==

    Compares two objects for equality.

## !=

    Compares two objects for inequality.

## >

    true if the left hand side is greater than the right hand side.

## >=

    true if the left hand side is greater or equal to the right hand side.

## <

    true if the left hand side is lower than the right hand side.

## <=

    true if the left hand side is lower or equal to the right hand side.

# Logic

For if statements, for filtering, and if expressions, it can be useful to combine multiple expressions.

# and

    For x and y, if x is false, then the value is x, else y. In a boolean context, this will be treated as True if both operands are truthy.

# or

    For x or y, if x is true, then the value is x, else y. In a boolean context, this will be treated as True if at least one operand is truthy.

# not

    For not x, if x is false, then the value is True, else False.

    Prefer negating is and in using their infix notation: foo is not bar instead of not foo is bar; foo not in bar instead of not foo in bar. All other expressions require prefix notation: not (foo and bar).

# (expr)

    Parentheses group an expression. This is used to change evaluation order, or to make a long expression easier to read or less ambiguous.

# Other Operators

The following operators are very useful but don’t fit into any of the other two categories:

# in

    Perform a sequence / mapping containment test. Returns true if the left operand is contained in the right. {{ 1 in [1, 2, 3] }} would, for example, return true.

# is

    Performs a test.

# | (pipe, vertical bar)

    Applies a filter.

# ~ (tilde)

    Converts all operands into strings and concatenates them.

    {{ "Hello " ~ name ~ "!" }} would return (assuming name is set to 'John') Hello John!.

# ()

    Call a callable: {{ post.render() }}. Inside of the parentheses you can use positional arguments and keyword arguments like in Python:

    {{ post.render(user, full=true) }}.

# . / []

    Get an attribute of an object. (See Variables)

