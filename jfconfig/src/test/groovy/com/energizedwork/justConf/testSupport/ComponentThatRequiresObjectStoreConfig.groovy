package com.energizedwork.justConf.testSupport

import com.energizedwork.justConf.testSupport.objectStore.ObjectStoreFactory

import javax.validation.Valid
import javax.validation.constraints.NotNull

class ComponentThatRequiresObjectStoreConfig {

    String optionalProperty

    @NotNull
    @Valid
    ObjectStoreFactory widgetStoreFactory

}
