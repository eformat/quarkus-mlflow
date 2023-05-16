package org.acme.model;


public class Storage {
    public String key;
    public String path;

    @Override
    public String toString() {
        return "Storage{" +
                "key='" + key + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
