package com.energizedwork.justConf.testSupport.objectStore

import com.fasterxml.jackson.annotation.JsonTypeName
import org.hibernate.validator.constraints.NotBlank

@JsonTypeName('filesystem')
class FilesystemObjectStoreFactory extends ObjectStoreFactory {

    @NotBlank
    String directory

    @Override
    ObjectStore getObjectStore() {
        new FilesystemObjectStore(directory)
    }
}
