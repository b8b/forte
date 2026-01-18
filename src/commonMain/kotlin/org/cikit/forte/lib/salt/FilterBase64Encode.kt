package org.cikit.forte.lib.salt

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.buildByteString
import kotlinx.io.bytestring.encode
import org.cikit.forte.core.FilterMethod
import org.cikit.forte.core.NamedArgs
import org.cikit.forte.core.typeName
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FilterBase64Encode : FilterMethod {
    override fun invoke(subject: Any?, args: NamedArgs): Any {
        args.requireEmpty()
        @OptIn(ExperimentalEncodingApi::class)
        return when (subject) {
            is ByteString -> Base64.UrlSafe.encode(subject)
            is ByteArray -> Base64.UrlSafe.encode(subject)
            is Iterable<*> -> {
                val result = buildByteString {
                    for (element in subject) {
                        if (element !is Number) {
                            throw IllegalArgumentException(
                                "non-numeric list element " +
                                        "of type '${typeName(element)}'"
                            )
                        }
                        append(element.toByte())
                    }
                }
                Base64.UrlSafe.encode(result)
            }

            else -> throw IllegalArgumentException(
                "invalid operand of type '${typeName(subject)}'"
            )
        }
    }
}
