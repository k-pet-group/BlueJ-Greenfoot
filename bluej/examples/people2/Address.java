/**
 ** Class Address - used to store address details for a post address
 ** 
 ** Author: Michael Kolling
 ** Date: 
 **/
public class Address
{
    private String street;
    private String town;
    private String postCode;
    private String country;

    /**
     ** Constructan Address without country
     **/
    public Address(String street, String town, String postCode)
    {
        this(street, town, postCode, "");
    }

    /**
     ** Constructan Address with full details
     **/
    public Address(String street, String town, String postCode, String country)
    {
        this.street = street;
        this.town = town;
        this.postCode = postCode;
        this.country = country;
    }

    /**
     ** Constructan Address with full details
     **/
    public String toString()
    {
        return street + "\n" +
               town + " " + postCode + "\n" +
               country + "\n";
    }
}
