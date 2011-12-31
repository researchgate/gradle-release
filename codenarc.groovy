
ruleset {

    description 'Gradle Release plugin CodeNarc RuleSet'

    ruleset( 'http://codenarc.sourceforge.net/StarterRuleSet-AllRulesByCategory.groovy.txt' ) {

        DuplicateNumberLiteral       ( enabled : false )
        DuplicateStringLiteral       ( enabled : false )
        BracesForTryCatchFinally     ( enabled : false )
        BuilderMethodWithSideEffects ( enabled : false )
        FactoryMethodName            ( enabled : false )
        MethodName                   ( enabled : false )
        LineLength                   ( length  : 160   )
    }
}
