package net.minecraft.core;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NonNullList<E> extends AbstractList<E> {
    private final List<E> list;
    private final E defaultValue;

    protected NonNullList(List<E> list, E defaultValue) {
        this.list = list;
        this.defaultValue = defaultValue;
    }

    public static <E> NonNullList<E> create() {
        return new NonNullList<>(new ArrayList<>(), null);
    }

    public static <E> NonNullList<E> withSize(int size, E defaultValue) {
        Objects.requireNonNull(defaultValue);
        List<E> l = new ArrayList<>(size);
        for (int i = 0; i < size; i++) l.add(defaultValue);
        return new NonNullList<>(l, defaultValue);
    }

    @Override public E get(int index) { return list.get(index); }
    @Override public int size() { return list.size(); }
    @Override public E set(int index, E element) {
        Objects.requireNonNull(element);
        return list.set(index, element);
    }
    @Override public void add(int index, E element) {
        Objects.requireNonNull(element);
        list.add(index, element);
    }
    @Override public E remove(int index) { return list.remove(index); }
    @Override public void clear() {
        if (defaultValue == null) list.clear();
        else {
            for (int i = 0; i < list.size(); i++) list.set(i, defaultValue);
        }
    }
}
