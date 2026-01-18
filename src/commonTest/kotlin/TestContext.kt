import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.Function
import org.cikit.forte.core.NamedArgs
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class TestContext {

    @Test
    fun testUpdateScope() = runTest {
        val ctx = Context.builder()
        ctx.defineFunction("sample") { args ->
            args.requireEmpty()
            42
        }

        val instance1 = MyFunc1(ctx)
        ctx.defineFunction("doit1", instance1)
        assertTrue(
            "instance of MyFunc1 does not depend on context",
            ctx.getFunction("doit1") === instance1
        )
        assertEquals(
            42,
            ctx.evalExpression(Forte.parseExpression("doit1()"))
        )
        // redefine sample function
        ctx.defineFunction("sample") { args ->
            args.requireEmpty()
            43
        }
        assertTrue(
            "instance of MyFunc1 depends on sample",
            ctx.getFunction("doit1")!! !== instance1
        )
        assertEquals(
            43,
            ctx.evalExpression(Forte.parseExpression("doit1()"))
        )

        val instance2 = MyFunc2(ctx)
        ctx.defineFunction("doit2", instance2)
        assertEquals(
            43,
            ctx.evalExpression(Forte.parseExpression("doit1()"))
        )
        val instance3 = ctx.getFunction("doit2")!!
        assertTrue(
            "instance of MyFunc2 depends on context",
            instance3 !== instance2
        )
        // redefine sample function
        ctx.defineFunction("sample") { args ->
            args.requireEmpty()
            44
        }
        assertTrue(
            "instance of MyFunc2 does not depend on sample",
            ctx.getFunction("doit2")!! === instance3
        )
        assertEquals(
            44,
            ctx.evalExpression(Forte.parseExpression("doit2()"))
        )

        val finalContext = ctx.build()
        assertTrue(
            "instance of MyFunc1 does not depend on context",
            finalContext.getFunction("doit1")!! === ctx.getFunction("doit1")!!
        )
        assertTrue(
            "instance of MyFunc2 depends on context",
            finalContext.getFunction("doit2")!! !== ctx.getFunction("doit2")!!
        )

        val builder =  Context.builder().withScope(finalContext)
        assertTrue(
            "instance of MyFunc1 does not depend on context",
            finalContext.getFunction("doit1")!! === builder.getFunction("doit1")!!
        )
        assertTrue(
            "instance of MyFunc2 depends on context",
            finalContext.getFunction("doit2")!! !== builder.getFunction("doit2")!!
        )

        builder.defineFunction("doit2", MyFunc2(builder))
        assertEquals(
            44,
            ctx.evalExpression(Forte.parseExpression("doit2()"))
        )
        builder.defineFunction("sample") { args ->
            args.requireEmpty()
            45
        }
        assertEquals(
            44,
            ctx.evalExpression(Forte.parseExpression("doit2()"))
        )
        assertEquals(
            45,
            builder.evalExpression(Forte.parseExpression("doit2()"))
        )
    }

    private class MyFunc1(val sample: Function) : Function, DependencyAware {
        constructor(ctx: Context<*>) : this(
            ctx.getFunction("sample")
                ?: error("function 'sample' is not defined")
        )

        override fun withDependencies(ctx: Context<*>): DependencyAware {
            val sample = ctx.getFunction("sample")
                ?: error("function 'sample' is not defined")
            if (sample != this.sample) {
                return MyFunc1(sample)
            }
            return this
        }

        override fun invoke(args: NamedArgs): Any? {
            return sample(args)
        }
    }

    private class MyFunc2(val ctx: Context<*>) : Function, DependencyAware {
        override fun withDependencies(ctx: Context<*>): DependencyAware {
            if (ctx != this.ctx) {
                return MyFunc2(ctx)
            }
            return this
        }

        override fun invoke(args: NamedArgs): Any? {
            return ctx.getFunction("sample")!!(args)
        }
    }

}
