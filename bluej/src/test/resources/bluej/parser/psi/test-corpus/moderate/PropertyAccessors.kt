package test.moderate

class Person(firstName: String, lastName: String) {
    var name: String = "$firstName $lastName"
        get() = field.uppercase()
        set(value) {
            field = value.trim()
        }
    
    val initials: String
        get() = name.split(" ").map { it.first() }.joinToString("")
    
    var age: Int = 0
        private set
    
    fun celebrateBirthday() {
        age++
    }
}