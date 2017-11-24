package com.energizedwork.justConf.testSupport.objectStore

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName('memory')
class MemoryObjectStoreFactory extends ObjectStoreFactory {

    @Override
    ObjectStore getObjectStore() {
        new MemoryObjectStore()
    }
}
