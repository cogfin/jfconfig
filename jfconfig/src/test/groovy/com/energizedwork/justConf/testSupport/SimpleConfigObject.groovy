package com.energizedwork.justConf.testSupport

import com.fasterxml.jackson.annotation.JsonIgnore
import io.dropwizard.Configuration
import org.hibernate.validator.constraints.NotBlank

import javax.validation.constraints.NotNull

class SimpleConfigObject extends Configuration {

    String property1

    @NotNull
    String notNullProperty

    @NotBlank
    String notBlankProperty

    @NotNull
    @NotBlank
    String notNullOrBlankProperty

    @JsonIgnore
    String ignoredProperty

}
