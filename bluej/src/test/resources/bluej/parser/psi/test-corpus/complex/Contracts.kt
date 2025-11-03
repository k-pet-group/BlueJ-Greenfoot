package test.complex

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun isNotNull(value: Any?): Boolean {
    contract { returns(true) implies (value != null) }
    return value != null
}