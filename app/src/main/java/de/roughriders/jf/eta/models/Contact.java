package de.roughriders.jf.eta.models;

/**
 * Created by b0wter on 6/11/16.
 */
public class Contact {
    public String name;
    public String phone;

    private static final String SERIALIZATION_FIELD_DELIMITER = ";";

    public Contact(String name, String phone){
        this.name = name;
        this.phone = phone;
    }

    public static Contact fromString(String s){
        String[] parts = s.split(SERIALIZATION_FIELD_DELIMITER);
        if(parts.length != 2)
            throw new IllegalArgumentException("The string is not formatted correctly.");
        return new Contact(parts[0], parts[1]);
    }
}
