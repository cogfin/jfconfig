package com.energizedwork.justConf.testSupport.objectStore

class S3ObjectStore implements ObjectStore {

    S3ObjectStore(String accessKey, String secret, String endPoint, String bucket) {
    }

    @Override
    byte[] get(String key) {
        return new byte[0]
    }

    @Override
    void put(String key, byte[] object) {

    }

}
