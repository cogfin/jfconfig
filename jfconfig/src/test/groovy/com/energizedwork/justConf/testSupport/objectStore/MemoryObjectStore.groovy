package com.energizedwork.justConf.testSupport.objectStore

class MemoryObjectStore implements ObjectStore {

    Map<String, byte[]> store = new HashMap<String, byte[]>()

    @Override
    byte[] get(String key) {
        store[key]
    }

    @Override
    void put(String key, byte[] object) {
        store[key] = object
    }

}
