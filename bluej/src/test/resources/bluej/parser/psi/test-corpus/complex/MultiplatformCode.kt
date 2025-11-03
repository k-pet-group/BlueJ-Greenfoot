package test.complex

expect class Platform() {
    fun name(): String
}

expect fun platformSpecific(): String