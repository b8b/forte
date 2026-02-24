package org.cikit.forte.lib.salt

import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.Suspended
import org.cikit.forte.emitter.YamlEmitter
import org.cikit.forte.lib.common.suspendingEmit

class FilterToYaml: FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        val target = StringBuilder()
        val emitter = YamlEmitter(target)
        return Suspended { ctx ->
            emitter.suspendingEmit(ctx, subject)
            emitter.close()
            target.toString()
        }
    }
}
