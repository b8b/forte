import kotlinx.coroutines.test.runTest
import org.cikit.forte.Forte
import org.cikit.forte.core.Context
import org.cikit.forte.core.DependencyAware
import org.cikit.forte.core.Function
import org.cikit.forte.core.NamedArgs
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TestContext {

    @Test
    fun testRedefine1() {
        // test updating a non-dependency aware implementation
        val ctx = Context.builder()

        assertFails("expected that doit1 cannot resolve sample") {
            ctx.defineFunction("doit1", MyFunc1(ctx))
        }

        ctx.defineFunction("sample") { args ->
            args.requireEmpty()
            42
        }
        val initial = MyFunc1(ctx)
        ctx.defineFunction("doit1", initial)
        assertTrue(
            "expected that initial instance is now in ctx",
            initial === ctx.getFunction("doit1")!!
        )

        // as doit1 depends on sample, it has to be updated when
        // sample is redefined
        ctx.defineFunction("sample") { args ->
            args.requireEmpty()
            43
        }
        assertTrue(
            "expected that initial instance has been updated in ctx",
            initial !== ctx.getFunction("doit1")!!
        )
    }

    @Test
    fun testRedefine2() {
        // test updating a dependency aware implementation
        val ctx = Context.builder()

        ctx.defineFunction("sample") { args ->
            args.requireEmpty()
            42
        }
        val initialSample = ctx.getFunction("sample")!!
        ctx.defineFunction("doit1", MyFunc1(ctx))
        ctx.defineFunction("doit2", MyFunc2(ctx))
        val initialDoit2 = ctx.getFunction("doit2")!!
        // doit2 -> doit1 -> sample
        ctx.defineFunction("doit1", MyFunc1(ctx))

        assertTrue(
            "expect sample stays the same",
            initialSample === ctx.getFunction("sample")!!
        )

        assertTrue(
            "expect doit2 has been updated",
            initialDoit2 !== ctx.getFunction("doit2")!!
        )
    }

    @Test
    fun testCycle() {
        val ctx = Context.builder()
        ctx.defineFunction("sample") { args ->
            args.requireEmpty()
            42
        }
        assertFails("expected a cyclic dependency") {
            ctx.defineFunction("sample", MyFunc1(ctx))
        }
    }

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
            "instance of MyFunc1 depends on sample which is already resolved",
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
        assertTrue(
            "instance of MyFunc2 depends on doit1 which is already resolved",
            ctx.getFunction("doit2") === instance2
        )

        // redefine sample function
        ctx.defineFunction("sample") { args ->
            args.requireEmpty()
            44
        }
        assertTrue(
            "instance of MyFunc2 depends on sample indirectly",
            ctx.getFunction("doit2")!! !== instance2
        )
        assertEquals(
            44,
            ctx.evalExpression(Forte.parseExpression("doit2()"))
        )

        val finalContext = ctx.build()
        assertTrue(
            "finalized context has the same function sample",
            ctx.getFunction("sample")!! === finalContext.getFunction("sample")!!
        )
        assertTrue(
            "finalized context has the same function doit1",
            ctx.getFunction("doit1")!! === finalContext.getFunction("doit1")!!
        )
        assertTrue(
            "finalized context has the same function doit2",
            ctx.getFunction("doit2")!! === finalContext.getFunction("doit2")!!
        )

        val newScope = Context.builder().withScope(finalContext)
        assertTrue(
            "newScope context has the same function sample",
            ctx.getFunction("sample")!! === newScope.getFunction("sample")!!
        )
        assertTrue(
            "newScope context has the same function doit1",
            ctx.getFunction("doit1")!! === newScope.getFunction("doit1")!!
        )
        assertTrue(
            "newScope context has the same function doit2",
            ctx.getFunction("doit2")!! === newScope.getFunction("doit2")!!
        )

        // redefine doit1
        newScope.defineFunction("doit1", MyFunc1(newScope))
        assertTrue(
            "instance of MyFunc2 depends on doit1",
            ctx.getFunction("doit2")!! !== newScope.getFunction("doit2")!!
        )

        // redefine doit1
        newScope.defineFunction("doit1", MyFunc1(newScope))
        assertTrue(
            "instance of MyFunc2 depends on doit1",
            ctx.getFunction("doit2")!! !== newScope.getFunction("doit2")!!
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
            if (sample !== this.sample) {
                return MyFunc1(sample)
            }
            return this
        }

        override fun invoke(args: NamedArgs): Any? {
            return sample(args)
        }
    }

    private class MyFunc2(val myFunc1: Function) : Function, DependencyAware {
        constructor(ctx: Context<*>) : this(
            ctx.getFunction("doit1")
                ?: error("function 'doit1' is not defined")
        )

        override fun withDependencies(ctx: Context<*>): DependencyAware {
            val myFunc1 = ctx.getFunction("doit1")
                ?: error("function 'doit1' is not defined")
            if (myFunc1 !== this.myFunc1) {
                return MyFunc2(myFunc1)
            }
            return this
        }

        override fun invoke(args: NamedArgs): Any? {
            return myFunc1(args)
        }
    }

}
