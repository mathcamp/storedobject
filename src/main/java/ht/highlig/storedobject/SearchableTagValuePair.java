package ht.highlig.storedobject;

/**
 * Created by bkase on 6/24/14.
 */
public class SearchableTagValuePair {
    public final String key;
    public final String value;

    public SearchableTagValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "key" + key +
                "value" + value + "}";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SearchableTagValuePair &&
                key.equals(((SearchableTagValuePair)other).key) &&
                value.equals(((SearchableTagValuePair)other).value);
    }
}
