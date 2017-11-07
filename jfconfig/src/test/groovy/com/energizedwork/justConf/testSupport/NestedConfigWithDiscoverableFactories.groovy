package com.energizedwork.justConf.testSupport

import com.energizedwork.justConf.testSupport.objectStore.ObjectStoreFactory
import io.dropwizard.Configuration

import javax.validation.Valid
import javax.validation.constraints.NotNull

class NestedConfigWithDiscoverableFactories extends Configuration {

    @NotNull
    @Valid
    ObjectStoreFactory imageStoreFactory

    @Valid
    ObjectStoreFactory someOtherStuff

    @NotNull
    @Valid
    SomeThirdPartyServiceConfiguration thirdPartyConfiguration

    @Valid
    ComponentThatRequiresObjectStoreConfig component

}
