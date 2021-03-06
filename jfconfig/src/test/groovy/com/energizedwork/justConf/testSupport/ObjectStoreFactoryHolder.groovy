package com.energizedwork.justConf.testSupport

import com.energizedwork.justConf.testSupport.objectStore.ObjectStoreFactory
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.sun.istack.internal.NotNull

import javax.validation.Valid

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'id')
class ObjectStoreFactoryHolder {
    String id
    @NotNull
    @Valid
    ObjectStoreFactory factory
}
