package com.energizedwork.justConf.testSupport.objectStore

class FilesystemObjectStore implements ObjectStore {

    private final File directory

    FilesystemObjectStore(String directory) {
        this.directory = new File(directory)
    }

    @Override
    byte[] get(String key) {
        new File(directory, key.toString()).bytes
    }

    @Override
    void put(String key, byte[] object) {
        new File(directory, key).bytes = object
    }

}
