package com.energizedwork.justConf.testSupport.objectStore

import com.fasterxml.jackson.annotation.JsonTypeName
import org.hibernate.validator.constraints.NotBlank

@JsonTypeName('database')
class DbObjectStoreFactory extends ObjectStoreFactory {

    @NotBlank
    String url

    @NotBlank
    String username

    @NotBlank
    String password

    @NotBlank
    String table

    @Override
    ObjectStore getObjectStore() {
        new DbObjectStore(url, username, password, table)
    }
}
