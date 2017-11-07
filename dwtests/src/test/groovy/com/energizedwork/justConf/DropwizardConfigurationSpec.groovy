package com.energizedwork.justConf

import com.energizedwork.justConf.testSupport.ConfigurationTestApplication

class DropwizardConfigurationSpec extends InheritanceConfigurationSpec {

    def getConfiguration(Class configClass, String configPath, String importKey = null, File externalConfigFile = null) {
        def app = new ConfigurationTestApplication(configClass, importKey, externalConfigFile)
        app.run('configure', configPath)
        app.configuration
    }

}
