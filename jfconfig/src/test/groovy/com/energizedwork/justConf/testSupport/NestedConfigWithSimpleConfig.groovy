package com.energizedwork.justConf.testSupport

import com.energizedwork.justConf.testSupport.objectStore.ObjectStoreFactory
import io.dropwizard.Configuration

import javax.validation.Valid
import javax.validation.constraints.NotNull

class NestedConfigWithSimpleConfig extends Configuration {

    @NotNull
    @Valid
    ObjectStoreFactory imageStoreFactory

    @NotNull
    @Valid
    SimpleConfigObject simpleObject

}
