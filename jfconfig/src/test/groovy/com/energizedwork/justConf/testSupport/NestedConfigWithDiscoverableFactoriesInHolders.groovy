package com.energizedwork.justConf.testSupport

import io.dropwizard.Configuration

import javax.validation.Valid
import javax.validation.constraints.NotNull

class NestedConfigWithDiscoverableFactoriesInHolders extends Configuration {

    @NotNull
    @Valid
    ObjectStoreFactoryHolder imageStoreFactory

    @Valid
    ObjectStoreFactoryHolder someOtherStuff

    @NotNull
    @Valid
    SomeThirdPartyServiceConfiguration thirdPartyConfiguration

    @Valid
    ComponentThatRequiresObjectStoreConfig component

}
