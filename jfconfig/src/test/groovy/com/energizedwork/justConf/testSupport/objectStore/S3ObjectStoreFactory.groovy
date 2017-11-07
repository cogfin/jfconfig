package com.energizedwork.justConf.testSupport.objectStore

import com.fasterxml.jackson.annotation.JsonTypeName
import org.hibernate.validator.constraints.NotBlank

@JsonTypeName('s3')
class S3ObjectStoreFactory extends ObjectStoreFactory {

    @NotBlank
    String accessKey

    @NotBlank
    String secret

    @NotBlank
    String endpoint

    @NotBlank
    String bucket

    @Override
    ObjectStore getObjectStore() {
        new S3ObjectStore(accessKey, secret, endpoint, bucket)
    }
}
