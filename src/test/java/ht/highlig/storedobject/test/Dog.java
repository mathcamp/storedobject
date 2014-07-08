package ht.highlig.storedobject.test;

import com.google.gson.annotations.Expose;

import java.util.Arrays;
import java.util.List;

import ht.highlig.storedobject.Database;
import ht.highlig.storedobject.SearchableTagValuePair;

/**
 * Created by bkase on 7/7/14.
 */
public class Dog implements Database.StoredObject {
    @Expose
    public final String name;
    @Expose
    public final String id;
    @Expose
    public final int age;
    @Expose
    public final boolean is_striped;

    public Dog(String name, String id, int age, boolean is_striped) {
        this.name = name;
        this.id = id;
        this.age = age;
        this.is_striped = is_striped;
    }

    @Override
    public TYPE getStoredObjectType() {
        return ht.highlig.storedobject.test.TYPE.dog;
    }

    @Override
    public String getStoredObjectId() {
        return id;
    }

    @Override
    public List<SearchableTagValuePair> getStoredObjectSearchableTags() {
        return Arrays.asList(
                new SearchableTagValuePair("name", name)
        );
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
                ",age" + age +
                ",is_striped" + is_striped + "}";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Dog &&
                this.age == ((Dog) other).age &&
                this.id.equals(((Dog) other).id) &&
                this.name.equals(((Dog) other).name) &&
                this.is_striped == ((Dog) other).is_striped;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
