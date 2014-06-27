package ht.highlig.storedobject.test;

// import org.robolectric.Robolectric;

import android.app.Activity;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

import ht.highlig.storedobject.Database;

import static junit.framework.Assert.assertEquals;

/**
 * Created by revant on 6/24/14.
 */
@RunWith(RobolectricTestRunner.class)
public class DatabaseTest {

    private Context context;

    @Before
    public void setup() {
        context = new Activity();
    }

    @Test
    public void testLoadOfSaveOfXisX() {
        Person p = new Person("yo", "an id", "http://aurl", 4, true);
        Database db = Database.with(context);
        db.saveObject(p);
        Person result = db.load(TYPE.person).getFirst();

        assertEquals(p, result);
    }

    @Test
    public void testLoadOfSaveOfXYisXY() {
        Person x = new Person("yo", "an id", "http://aurl", 4, true);
        Person y = new Person("yo2", "another id", "http://anotherurl", 5, false);
        Database db = Database.with(context);
        List<Person> ps = Arrays.asList(x, y);
        db.saveObjects(ps);
        List<Person> results = (List<Person>)(List<?>)db.load(TYPE.person).execute();

        assertEquals(ps, results);
    }

    @Test
    public void testSortedLoad() {
        Person x = new Person("frank", "an id", "http://aurl", 5, true);
        Person y = new Person("fred", "another id", "http://anotherurl", 4, false);
        Person z = new Person("james", "some id", "http://someurl", 7, true);
        Database db = Database.with(context);
        List<Person> ps = Arrays.asList(x, y, z);
        List<Person> results = (List<Person>)(List<?>)db.load(TYPE.person)
                .orderByTs(Database.SORT_ORDER.DESC)
                .execute();

        assertEquals(results.get(0), z);
        assertEquals(results.get(1), y);
        assertEquals(results.get(2), x);

        System.out.println(results);
    }
}
