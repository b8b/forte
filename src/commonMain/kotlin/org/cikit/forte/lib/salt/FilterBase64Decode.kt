package org.cikit.forte.lib.salt

import kotlinx.io.bytestring.decodeToByteString
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FilterBase64Decode : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        @OptIn(ExperimentalEncodingApi::class)
        return when (subject) {
            is CharSequence -> Base64.decodeToByteString(subject)

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
