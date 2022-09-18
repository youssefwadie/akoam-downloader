package com.github.youssefwadie.akoamdownloader.model;

import java.net.URI;
import java.util.Objects;


public final class Episode {
    private final int number;
    private final String name;
    private final URI uri;

    public Episode(int number, String name, URI uri) {
        this.number = number;
        this.name = name;
        this.uri = uri;
    }

    public int number() {
        return number;
    }

    public String name() {
        return name;
    }

    public URI uri() {
        return uri;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Episode) obj;
        return this.number == that.number &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, name, uri);
    }

    @Override
    public String toString() {
        return "Episode[" +
                "number=" + number + ", " +
                "name=" + name + ", " +
                "uri=" + uri + ']';
    }

}
