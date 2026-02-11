# Test Python Methods

## Slice

    {% assert_that "abcdefghijk"[1:3:-1]    is eq('') %}
    {% assert_that "abcdefghijk"[3:1:-1]    is eq('dc') %}
    {% assert_that "abcdefghijk"[3:1:-2]    is eq('d') %}
    {% assert_that "abcdefghijk"[3:1]       is eq('') %}
    {% assert_that "abcdefghijk"[3:1:-1]    is eq('dc') %}
    {% assert_that "abcdefghijk"[3::-1]     is eq('dcba') %}
    {% assert_that "abcdefghijk"[8:2:-1]    is eq('ihgfed') %}
    {% assert_that "abcdefghijk"[8:2:-2]    is eq('ige') %}
    {% assert_that "abcdefghijk"[8::-2]     is eq('igeca') %}
    {% assert_that "abcdefghijk"[-2:-5:-2]  is eq('jh') %}
    {% assert_that "abcdefghijk"[-2:-4:-2]  is eq('j') %}
    {% assert_that "abcdefghijk"[199:-4:-2] is eq('ki') %}
    {% assert_that "abcdefghijk"[199:11:-1] is eq('') %}
    {% assert_that "abcdefghijk"[199:10:-1] is eq('') %}
    {% assert_that "abcdefghijk"[199:9:-1]  is eq('k') %}
    {% assert_that "abcdefghijk"[-1:-2:-1]  is eq('k') %}
    {% assert_that "abcdefghijk"[-10:-2:-1] is eq('') %}

## startswith

    {% assert_true "12345".startswith("") %}
    {% assert_true "12345".startswith("1234") %}
    {% assert_true "12345".startswith("123") %}
    {% assert_true "12345".startswith("12") %}
    {% assert_true "12345".startswith("1") %}
    {% assert_true "12345".startswith("") %}

    {% assert_false "12345".startswith("1", 0, 0) %}
    {% assert_true "12345".startswith("", 0, 0) %}
    {% assert_true "12345".startswith("345", -3) %}
