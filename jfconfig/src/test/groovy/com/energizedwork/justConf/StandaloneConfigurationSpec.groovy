package com.energizedwork.justConf

class StandaloneConfigurationSpec extends InheritanceConfigurationSpec {

    def getConfiguration(Class configClass, String configPath, String importKey = null, File externalConfigFile = null) {
        // mimic cli which prints stacktrace
        try {
            def classpathSource = JFConfig.createEnvVarSubstitutingClasspathSourceProvider()
            JFConfig.fromSourceProvider(classpathSource, configClass, configPath, "inherits", importKey, externalConfigFile, "xx")
        } catch (e) {
            e.printStackTrace()
            throw new RuntimeException(e)
        }
    }

}
