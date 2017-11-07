package com.energizedwork.justConf.testSupport.objectStore

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import io.dropwizard.jackson.Discoverable

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'id', scope = ObjectStoreFactory)
abstract class ObjectStoreFactory implements Discoverable {
    String id
    abstract ObjectStore getObjectStore()
}
