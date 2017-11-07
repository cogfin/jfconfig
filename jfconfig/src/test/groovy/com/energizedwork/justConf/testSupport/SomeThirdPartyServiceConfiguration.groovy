package com.energizedwork.justConf.testSupport

import org.hibernate.validator.constraints.NotBlank

class SomeThirdPartyServiceConfiguration {

    @NotBlank
    String aThing

    @NotBlank
    String anotherThing

}
