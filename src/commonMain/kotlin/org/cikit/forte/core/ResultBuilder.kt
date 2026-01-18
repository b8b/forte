package org.cikit.forte.core

import kotlinx.coroutines.flow.FlowCollector

sealed class ResultBuilder {

    data object Discard : ResultBuilder()

    class Render(
        val collector: FlowCollector<CharSequence>
    ) : ResultBuilder(), FlowCollector<CharSequence> by collector

    class Emit(
        val collector: FlowCollector<Any?>
    ) : ResultBuilder(), FlowCollector<Any?> by collector

}
