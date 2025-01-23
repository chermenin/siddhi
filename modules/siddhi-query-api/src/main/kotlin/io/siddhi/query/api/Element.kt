package io.siddhi.query.api

import java.io.Serializable

/**
 * Basic elements.
 */
abstract class Element : Serializable {

    private var context: Context? = null

    fun getContext(): Context? {
        return context
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> setContext(context: Context?): T {
        this.context = this.context.update(context)
        return this as T
    }
}

/**
 * Location in the code.
 */
data class ScriptIndex(val line: Int, val position: Int)

/**
 * Code context.
 */
data class Context(
    val startIndex: ScriptIndex? = null,
    val endIndex: ScriptIndex? = null
)

fun Context?.update(context: Context?): Context {
    return Context(
        if (this?.startIndex == null && context?.startIndex != null) {
            context.startIndex
        } else {
            this?.startIndex
        },
        if (this?.endIndex == null && context?.endIndex != null) {
            context.endIndex
        } else {
            this?.endIndex
        },
    )
}