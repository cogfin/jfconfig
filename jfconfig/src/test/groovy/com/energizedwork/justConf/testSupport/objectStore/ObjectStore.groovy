package com.energizedwork.justConf.testSupport.objectStore

interface ObjectStore {
    byte[] get(String key)
    void put(String key, byte[] object)
}
