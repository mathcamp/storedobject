package ht.highlig.storedobject.test;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import ht.highlig.storedobject.Database;
import ht.highlig.storedobject.SearchableTagValuePair;

/**
 * Created by bkase on 6/24/14.
 */
public class Person implements Database.StoredObject {
    @Expose
    public final String name;
    @Expose
    public final String id;
    @Expose
    public final String image_url;
    @Expose
    public final int age;
    @Expose
    public final boolean is_real;

    public Person(String name, String id, String image_url, int age, boolean is_real) {
        this.name = name;
        this.id = id;
        this.image_url = image_url;
        this.age = age;
        this.is_real = is_real;
    }

    @Override
    public TYPE getStoredObjectType() {
        return ht.highlig.storedobject.test.TYPE.person;
    }

    /** Unique id for the object. **/
    @Override
    public String getStoredObjectId() {
        return id;
    }

    /** Return a list of tags that you want to be able to search for this object by **/
    @Override
    public List<SearchableTagValuePair> getStoredObjectSearchableTags() {
        List<SearchableTagValuePair> persons = new ArrayList<SearchableTagValuePair>();
        persons.add(new SearchableTagValuePair("name", name));
        return persons;
    }

    @Override
    public Long getStoredObjectTimestampMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "name: " + name +
                ",id" + id +
                ",image_url" + image_url +
                ",age" + age +
                ",is_real" + is_real + "}";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Person &&
                this.is_real == ((Person) other).is_real &&
                this.age == ((Person) other).age &&
                this.id.equals(((Person) other).id) &&
                this.name.equals(((Person) other).name) &&
                this.image_url.equals(((Person) other).image_url);
    }
}
