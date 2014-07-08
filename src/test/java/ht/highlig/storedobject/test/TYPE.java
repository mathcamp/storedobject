package ht.highlig.storedobject.test;

import ht.highlig.storedobject.Database;

/**
 * Created by bkase on 6/24/14.
 */
public enum TYPE implements Database.StoredObject.TYPE {
    person(Person.class),
    dog(Dog.class);

    private final Class cls;

    private TYPE(Class cls) {
        this.cls = cls;
    }

    @Override
    public String getTypeName() {
        return "person";
    }

    @Override
    public Class getTypeClass() {
        return cls;
    }
}
