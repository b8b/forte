from jinja2 import Environment, nodes, UndefinedError, DictLoader
from jinja2.ext import Extension
from jinja2.parser import Parser
from jinja2.runtime import Undefined

class AssertExtension(Extension):
    """Extension for {% assert %} tag.
    
    Usage: {% assert test_name(args) %}body{% endassert %}
    The body result is tested with: test_name(body_result, args)
    """
    
    tags = {'assert'}
    
    def parse(self, parser: Parser):
        lineno = next(parser.stream).lineno
        
        # Parse test name
        test_name = parser.stream.expect('name').value
        
        # Parse optional arguments (function call style)
        args = []
        if parser.stream.current.test('lparen'):
            parser.stream.expect('lparen')
            while not parser.stream.current.test('rparen'):
                args.append(parser.parse_expression())
                if parser.stream.current.test('comma'):
                    parser.stream.expect('comma')
            parser.stream.expect('rparen')
        
        # Parse body content until endassert
        body = parser.parse_statements(('name:endassert',), drop_needle=True)

        # Create a CallBlock that captures the body and applies the test
        return nodes.CallBlock(
            self.call_method('_do_assert', [nodes.Const(test_name), nodes.List(args)], lineno=lineno),
            [], [], body, lineno=lineno
        )
    
    def _do_assert(self, test_name, test_args, caller):
        """Execute body and assert test passes."""
        # First pass: force eager evaluation
        dummy_render = caller()
        # Second pass: str() forces full evaluation
        result = str(caller())
        
        # Get the test function from environment
        if test_name in self.environment.tests:
            test_func = self.environment.tests[test_name]
            
            # Evaluate args (they are AST nodes, need to evaluate them)
            evaluated_args = []
            for arg in test_args:
                if hasattr(arg, 'as_const'):
                    evaluated_args.append(arg.as_const())
                else:
                    # For dynamic args, we'd need the context - for now assume constants
                    evaluated_args.append(arg)
            
            # Call test with body result as first arg, then additional args
            if evaluated_args:
                passed = test_func(result, *evaluated_args)
            else:
                passed = test_func(result)
        else:
            raise RuntimeError(f"Unknown test: {test_name}")
        
        if not passed:
            raise AssertionError(f"Assertion failed on {test_name}({result!r})")
        return ""


class AssertFailsExtension(Extension):
    tags = {'assert_fails'}

    def parse(self, parser):
        lineno = next(parser.stream).lineno
        parser.stream.expect('name:as')
        var_name = parser.stream.expect('name').value
        body = parser.parse_statements(('name:endassert',), drop_needle=True)

        return nodes.CallBlock(
            self.call_method('_run_fails_body', [nodes.Const(var_name), nodes.ContextReference()],
                           lineno=lineno),
            [], [], body,  # Pass raw body nodes
            lineno=lineno
        )

    def _run_fails_body(self, var_name, context, caller):
        """Force eager execution by rendering body twice."""
        try:
            # First pass: force eager evaluation
            dummy_render = caller()
            # Second pass: str() forces full evaluation
            result = str(caller())
            raise TemplateRuntimeError(f"assert_fails: no exception, got '{result}'")
        except Exception as e:
            context.vars[var_name] = str(e)
            context.exported_vars.add(var_name)
            return ""

class AssertTrueExtension(Extension):
    """Extension for {% assert_true %} tag."""
    
    tags = {'assert_true'}
    
    def parse(self, parser: Parser):
        lineno = next(parser.stream).lineno
        
        # Parse the condition expression
        condition = parser.parse_expression()
        
        # Create output node
        return nodes.Output([
            self.call_method('_do_assert_true', [condition], lineno=lineno)
        ], lineno=lineno)
    
    def _do_assert_true(self, condition):
        """Assert condition is true."""
        if not condition:
            raise AssertionError(f"Expected condition to be true, but got {condition!r}")
        return ''

class AssertThatExtension(Extension):
    """Extension for {% assert_that %} tag."""

    tags = {'assert_that'}

    def parse(self, parser: Parser):
        lineno = next(parser.stream).lineno

        # Parse the condition expression
        condition = parser.parse_expression()

        # Validate: must be exactly "expr is test[(args)]"
        expr_node, test_name, test_args = self._validate_test_structure(condition, parser, lineno)

        # Create output node
        return nodes.Output([
            self.call_method('_do_assert_that', [expr_node, condition], lineno=lineno)
        ], lineno=lineno)

    def _validate_test_structure(self, node, parser, lineno):
        """Validate node is exactly: expr is test[(args)]"""
        if not isinstance(node, nodes.Test):
            parser.fail("assert_that requires 'expr is test[(args)]' syntax", lineno)

        if not isinstance(node.node, nodes.Expr):
            parser.fail("Left side must be expression before 'is'", lineno)

        expr = node.node
        test_name = node.name
        test_args = node.args or []

        # Optional validation: test must exist in environment
        if test_name not in self.environment.tests:
            parser.fail(f"Unknown test '{test_name}'", lineno)

        return expr, test_name, test_args

    def _do_assert_that(self, expr_node, condition):
        """Assert condition is true."""
        if not condition:
            raise AssertionError(
                f"assert_that failed on {expr_node!r}"
            )
        return ''

def create_test_environment():
    """Create a Jinja2 environment with test extensions."""
    env = Environment(
        loader=DictLoader({template_name: template_src}),
        extensions=[
            AssertExtension,
            AssertFailsExtension,
            AssertThatExtension,
            AssertTrueExtension,
        ]
    )
    
    # Register test functions
    def contains_test(container, value):
        """Test if container contains value (inverse of 'in' test).
        
        Usage: {% assert contains(value) %}{{ container }}{% endassert %}
        This is equivalent to: value in container
        """
        return value in container
    
    env.tests['contains'] = contains_test

    return env

env = create_test_environment()
template = env.get_template(template_name)
template.render()
