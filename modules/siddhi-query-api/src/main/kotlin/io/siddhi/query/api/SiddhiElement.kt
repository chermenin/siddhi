package io.siddhi.query.api

import java.io.Serializable

/**
 * Siddhi query elements having context
 */
interface SiddhiElement : Serializable {
    var queryContextStartIndex: IntArray?
    var queryContextEndIndex: IntArray?
}
