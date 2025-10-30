package test.moderate

import kotlin.properties.Delegates

class Observable {
    var name: String by Delegates.observable("initial") { prop, old, new ->
        println("${prop.name} changed from $old to $new")
    }
    
    var count: Int by Delegates.vetoable(0) { _, _, new ->
        new >= 0
    }
}