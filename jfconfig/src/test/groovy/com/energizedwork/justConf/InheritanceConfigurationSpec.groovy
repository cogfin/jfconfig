package com.energizedwork.justConf

import com.energizedwork.justConf.testSupport.BoringConfigObject
import com.energizedwork.justConf.testSupport.NestedConfigWithDiscoverableFactories
import com.energizedwork.justConf.testSupport.NestedConfigWithDiscoverableFactoriesInHolders
import com.energizedwork.justConf.testSupport.NestedConfigWithSimpleConfig
import com.energizedwork.justConf.testSupport.SimpleConfigObject
import com.energizedwork.justConf.testSupport.WithObjectStore
import com.energizedwork.justConf.testSupport.objectStore.MemoryObjectStoreFactory
import org.junit.After
import org.junit.BeforeClass
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

abstract class InheritanceConfigurationSpec extends Specification {

    @Shared
    PrintStream sysOut
    @Shared
    PrintStream sysErr

    @BeforeClass
    void storeSystemOutputStreams() {
        sysOut = System.out
        sysErr = System.err
    }

    @After
    void restoreSystemOutputStrams() {
        System.err = sysErr
        System.out = sysOut
    }

    def "can load a simple config"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/all-props-present.yml")

        expect:
        config.property1 == 'value1'
        config.notNullProperty == 'value2'
        config.notBlankProperty == 'value3'
        config.notNullOrBlankProperty == 'value4'
        config.ignoredProperty == null
    }

    def "blank NotNull is ok"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/blank-for-not-null.yml")

        expect:
        config.notNullProperty == ''
    }

    @Unroll
    def "#validation property #propertyName in config #configPath"() {
        def error = captureSysError()

        when:
        getConfiguration(SimpleConfigObject, configPath)

        then:
        thrown Exception
        String errorOut = error.toString()
        errorOut.contains(configPath)
        errorOut.contains(propertyName)

        where:
        validation  | propertyName       | configPath
        'NotNull'   | 'notNullProperty'  | 'config/simple/not-null-is-missing.yml'
        'NotNull'   | 'notNullProperty'  | 'config/simple/not-null-has-no-value.yml'
        'NotBlank'  | 'notBlankProperty' | 'config/simple/not-blank-is-blank.yml'
        'NotBlank'  | 'notBlankProperty' | 'config/simple/not-blank-is-missing.yml'
    }

    def "can import external config"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/import.yml", "import")

        expect:
        config.property1 == 'value1'
        config.notNullProperty == 'value2'
        config.notBlankProperty == 'value3'
        config.notNullOrBlankProperty == 'value4'
        config.ignoredProperty == null
    }

    def "import will not override existing entries in the config"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/import-does-not-override.yml", "import")

        expect:
        config.property1 == 'value1'
    }

    @Unroll
    def "single import can be made optional - import file #importFile"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/import-optional-file-${importFile}.yml", "import")

        expect:
        config.property1 == 'value1'

        where:
        importFile << ['exists', 'missing']
    }

    def "Sensible message when fail to open top level config file"() {
        def error = captureSysError()

        when:
        getConfiguration(SimpleConfigObject, configPath)

        then:
        thrown Exception
        String errorOut = error.toString()
        errorOut.contains(configPath)
        errorOut.contains('Failed to open')

        where:
        configPath = 'config/missing.yml'
    }

    def "Sensible message when fail to open parent config"() {
        def error = captureSysError()

        when:
        getConfiguration(SimpleConfigObject, configPath)

        then:
        thrown Exception
        String errorOut = error.toString()
        errorOut.contains('Failed to open')
        errorOut.contains(configPath)
        errorOut.contains(missingConfig)

        where:
        configPath = 'config/simple/inherits-parent-missing.yml'
        missingConfig = 'config/simple/missing-parent.yml'
    }

    def "Sensible message when fail to open imported config"() {
        def error = captureSysError()

        when:
        getConfiguration(SimpleConfigObject, configPath, 'import')

        then:
        thrown Exception
        String errorOut = error.toString()
        errorOut.contains('Failed to open')
        errorOut.contains(configPath)
        errorOut.contains(missingConfig)

        where:
        configPath = 'config/simple/import-missing-file.yml'
        missingConfig = 'config/simple/missing-import-file.yml'
    }

    def "Sensible message when fail to open imported config from map import"() {
        def error = captureSysError()

        when:
        getConfiguration(SimpleConfigObject, configPath, 'import')

        then:
        thrown Exception
        String errorOut = error.toString()
        errorOut.contains('Failed to open')
        errorOut.contains(configPath)
        errorOut.contains(missingConfig)

        where:
        configPath = 'config/simple/import-missing-file-map.yml'
        missingConfig = 'config/simple/missing-import-file.yml'
    }

    def "can load a simple config with inheritance"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/inherits.yml")

        expect:
        config.property1 == 'value1'
        config.notNullProperty == 'value2'
        config.notBlankProperty == 'value3'
        config.notNullOrBlankProperty == 'value4'
        config.ignoredProperty == null
    }

    def "an externalConfig file can override everything"() {
        given:
        File exConfig = File.createTempFile(System.properties['java.io.tmpdir'], 'externalConfig')
        exConfig.deleteOnExit()
        exConfig.text = "---\n"
        propertyNames.each {
            exConfig << "${it}: ${it}\n"
        }
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/inherits.yml", 'no-imports', exConfig)

        expect:
        propertyNames.each {
            assert config[it] == it
        }

        where:
        propertyNames = ['property1', 'notNullProperty', 'notBlankProperty', 'notNullOrBlankProperty']
    }

    def "can import from any file in heirarchy"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/multi-import-child.yml", "get")

        expect:
        config.property1 == 'value1'
        config.notNullProperty == 'value2'
        config.notBlankProperty == 'value3'
        config.notNullOrBlankProperty == 'value4'
        config.ignoredProperty == null
    }

    def "import can remove parent and import keys"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/import-removes-parent-and-import-keys.yml", "import")

        expect:
        config.property1 == 'value1'
        config.notNullProperty == 'value2'
        config.notBlankProperty == 'value3'
        config.notNullOrBlankProperty == 'value4'
        config.ignoredProperty == null
    }

    def "can import from a sub object"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/simple/import-from-sub-tree.yml", "import")

        expect:
        config.property1 == 'value1'
        config.notNullProperty == 'value2'
        config.notBlankProperty == 'value3'
        config.notNullOrBlankProperty == 'value4'
    }

    @Unroll
    def "can import to a sub object for config #configName"() {
        given:
        NestedConfigWithDiscoverableFactories config = getConfiguration(NestedConfigWithDiscoverableFactories, "config/nested/${configName}.yml", "import")

        expect:
        config.component.widgetStoreFactory.url == 'db-url'

        where:
        configName << ['import-to-sub-object', 'import-to-deep-sub-object']
    }

    def "can import from many files"() {
        given:
        SimpleConfigObject config = getConfiguration(SimpleConfigObject, "config/multiImports/import.yml", "import")

        expect:
        config.property1 == 'value1'
        config.notNullProperty == 'value2'
        config.notBlankProperty == 'value3'
        config.notNullOrBlankProperty == 'value4'
        config.ignoredProperty == null
    }

    def "can load a nested config with factories from a single file with all options supplied"() {
        given:
        NestedConfigWithDiscoverableFactories config = getConfiguration(NestedConfigWithDiscoverableFactories, "config/nested/single-file-all-configured.yml")

        expect:
        config.imageStoreFactory.directory == '/var/images'
        config.someOtherStuff.accessKey == 'myAccessKey'
        config.thirdPartyConfiguration.aThing == 'Thing A'
        config.component.widgetStoreFactory.url == 'db-url'
    }

    def "can load a nested config with factories from a single file with optional component missing"() {
        expect:
        getConfiguration(NestedConfigWithDiscoverableFactories, "config/nested/single-file-all-configured.yml")
    }

    def "can load a nested config with aliases"() {
        given:
        NestedConfigWithDiscoverableFactoriesInHolders config = getConfiguration(NestedConfigWithDiscoverableFactoriesInHolders, "config/holders/aliases.yml")

        expect:
        config.imageStoreFactory.factory.directory == '/var/images'
        config.someOtherStuff.is(config.imageStoreFactory)
        config.someOtherStuff.factory.is(config.imageStoreFactory.factory)
        config.thirdPartyConfiguration.aThing == 'Thing A'
        config.component.widgetStoreFactory.url == 'db-url'
    }

    def "can load a nested config with aliases on discoverable subclsses"() {
        given:
        NestedConfigWithDiscoverableFactories config = getConfiguration(NestedConfigWithDiscoverableFactories, "config/nested/aliases.yml")

        expect:
        config.imageStoreFactory.directory == '/var/images'
        config.someOtherStuff.is(config.imageStoreFactory)
        config.thirdPartyConfiguration.aThing == 'Thing A'
        config.component.widgetStoreFactory.url == 'db-url'
    }

    @Unroll
    def "Nested config validation failure for #propertyName in config #configPath"() {
        def error = captureSysError()

        when:
        getConfiguration(NestedConfigWithDiscoverableFactories, configPath)

        then:
        thrown Exception
        String errorOut = error.toString()
        errorOut.contains(configPath)
        errorOut.contains(propertyName)

        where:
        propertyName                         | configPath
        'component.widgetStoreFactory'       | 'config/nested/single-file-optional-component-missing-required-dep.yml'
        'component.widgetStoreFactory.table' | 'config/nested/single-file-optional-component-dependency-not-valid.yml'
    }

    @Unroll
    def "Detect and throw exception for circular references: #description"() {
        def error = captureSysError()

        when:
        getConfiguration(SimpleConfigObject, configPath, 'import')

        then:
        thrown Exception
        String errorOut = error.toString()
        errorOut.toLowerCase().contains(errorMessageContains)
        errorOut.contains(configPath)

        where:
        description                 | errorMessageContains         | configPath
        'inheritance'               | 'circular inheritance'       | 'config/circularDependencyChecks/parent/config.yml'
        'imports'                   | 'circular import'            | 'config/circularDependencyChecks/imports/config.yml'
        'parents imports'           | 'circular import'            | 'config/circularDependencyChecks/imports/parents-imports-circular.yml'
    }

    def "can use an alias in a parent"() {
        given:
        NestedConfigWithDiscoverableFactories config = getConfiguration(NestedConfigWithDiscoverableFactories, "config/nested/config-with-anchor.yml")

        expect:
        config.imageStoreFactory.is(config.someOtherStuff)
    }

    def "can use an alias in a child"() {
        given:
        NestedConfigWithDiscoverableFactories config = getConfiguration(NestedConfigWithDiscoverableFactories, "config/nested/config-with-alias.yml")

        expect:
        config.imageStoreFactory.is(config.someOtherStuff)
//        config.thirdPartyConfiguration.aThing == config.thirdPartyConfiguration.anotherThing
    }

    def "can load polymorphic objects that do not need config"() {
        given:
        WithObjectStore config = getConfiguration(WithObjectStore, "config/zero-cfg-polymorphic.yml")

        expect:
        config.stuff instanceof MemoryObjectStoreFactory
    }

    def "can import a configuration and honour inheritance and imports"() {
        given:
        NestedConfigWithSimpleConfig config = getConfiguration(NestedConfigWithSimpleConfig, "config/import-tree/config.yml", "get")

        expect:
        config.simpleObject.property1 == 'value1'
        config.simpleObject.notNullProperty == 'value2'
        config.simpleObject.notBlankProperty == 'value3'
        config.simpleObject.notNullOrBlankProperty == 'value4'
        config.simpleObject.ignoredProperty == null
    }

    def "big precedence test"() {
        //
        // I
        // ^
        // |
        // G -> H        E -> F
        // ^             ^
        // |             |
        // A     ->      B -> C
        //                 -> D
        BoringConfigObject config = getConfiguration(BoringConfigObject, 'config/precedence/configA.yml', 'imports')

        expect:
        config.property1 == 'set in A'
        config.property2 == 'set in B'
        config.property3 == 'set in C'
        config.property4 == 'set in D'
        config.property5 == 'set in E'
        config.property6 == 'set in F'
        config.property7 == 'set in G'
        config.property8 == 'set in H'
        config.property9 == 'set in I'

        config.nested1.property1 == 'set in A'
        config.nested1.property2 == 'set in X'
        config.nested1.property3 == 'set in Y'

        config.nested2.property1 == 'set in A'
        config.nested2.property2 == 'set in ZZ'
    }

    ByteArrayOutputStream captureSysError() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        System.err = new PrintStream(baos)
        baos
    }

    abstract def getConfiguration(Class configClass, String configPath, String importKey, File externalConfigFile)

}
