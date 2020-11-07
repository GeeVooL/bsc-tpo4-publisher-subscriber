package com.mdevv.tpo4.messagingserver.utils;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Topics {
    private final Map<String, List<SelectionKey>> topics = new HashMap<>();

    public List<SelectionKey> get(String name) {
        return topics.get(name);
    }

    public Set<String> list() {
        return topics.keySet();
    }

    public void add(String name) throws Exception {
        if (topics.containsKey(name)) {
            throw new Exception("Key already exists");
        }

        topics.put(name, new ArrayList<>());
    }

    public void delete(String name) throws Exception {
        if (!topics.containsKey(name)) {
            throw new Exception("Key not found");
        }
        topics.remove(name);
    }

    public void deleteItem(SelectionKey item) {
        topics.forEach((s, selectionKeys) -> selectionKeys.remove(item));
    }

    public boolean exists(String name) {
        return topics.containsKey(name);
    }
}
