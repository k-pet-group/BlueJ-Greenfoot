package test.moderate

class Address {
    val street: String
    val city: String
    val zipCode: String
    
    constructor(street: String, city: String, zipCode: String) {
        this.street = street
        this.city = city
        this.zipCode = zipCode
    }
    
    constructor(fullAddress: String) {
        val parts = fullAddress.split(", ")
        this.street = parts[0]
        this.city = parts[1]
        this.zipCode = parts[2]
    }
}