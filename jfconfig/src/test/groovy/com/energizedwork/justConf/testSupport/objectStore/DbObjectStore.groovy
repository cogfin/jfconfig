package com.energizedwork.justConf.testSupport.objectStore

class DbObjectStore implements ObjectStore {

    final String url
    final String username
    final String password
    final String tableName

    DbObjectStore(String url, String username, String password, String tableName) {
        this.url = url
        this.username = username
        this.password = password
        this.tableName = tableName
    }

    @Override
    byte[] get(String key) {
        return new byte[0]
    }

    @Override
    void put(String key, byte[] object) {

    }

}
