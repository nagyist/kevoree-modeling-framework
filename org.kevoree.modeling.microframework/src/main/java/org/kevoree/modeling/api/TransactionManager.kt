package org.kevoree.modeling.api

import org.kevoree.modeling.api.time.TimeView
import org.kevoree.modeling.api.time.TimeTree

/**
 * Created by duke on 7/29/14.
 */

trait TransactionManager {
    fun createTransaction(): Transaction
    fun close()
}
trait Transaction {
    fun commit()
    fun close()
}
trait TimeTransaction : Transaction {

    fun time(timepoint: Long): TimeView

    fun globalTimeTree() : TimeTree

    fun timeTree(path : String) : TimeTree

}
