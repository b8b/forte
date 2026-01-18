# Python methods

## Slice

```Python
>>> "abcdefghijk"[1:3:-1]
''
>>> "abcdefghijk"[3:1:-1]
'dc'
>>> "abcdefghijk"[3:1:-2]
'd'
>>> "abcdefghijk"[3:1]
''
>>> "abcdefghijk"[3:1:-1]
'dc'
>>> "abcdefghijk"[3::-1]
'dcba'
>>> "abcdefghijk"[8:2:-1]
'ihgfed'
>>> "abcdefghijk"[8:2:-2]
'ige'
>>> "abcdefghijk"[8::-2]
'igeca'
>>> "abcdefghijk"[-2:-5:-2]
'jh'
>>> "abcdefghijk"[-2:-4:-2]
'j'
>>> "abcdefghijk"[199:-4:-2]
'ki'
>>> "abcdefghijk"[199:11:-1]
''
>>> "abcdefghijk"[199:10:-1]
''
>>> "abcdefghijk"[199:9:-1]
'k'
>>> "abcdefghijk"[-1:-2:-1]
'k'
>>> "abcdefghijk"[-10:-2:-1]
''
```

## startswith

```Twig
>>> "12345".startswith("")
'true'
>>> "12345".startswith("1234")
'true'
>>> "12345".startswith("123")
'true'
>>> "12345".startswith("12")
'true'
>>> "12345".startswith("1")
'true'
>>> "12345".startswith("")
'true'
>>> "---"
'---'
>>> "12345".startswith("1", 0, 0)
'false'
>>> "12345".startswith("", 0, 0)
'true'
>>> "12345".startswith("345", -3)
'true'
```
