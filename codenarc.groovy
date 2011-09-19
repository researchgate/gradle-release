
ruleset {

    description 'Gradle Release plugin CodeNarc RuleSet'

    ruleset( "http://codenarc.sourceforge.net/StarterRuleSet-AllRulesByCategory.groovy.txt" ) {

        DuplicateNumberLiteral   ( enabled : false )
        DuplicateStringLiteral   ( enabled : false )
        BracesForTryCatchFinally ( enabled : false )
        Println                  ( enabled : false )

        LineLength               ( length  : 160   )
        MethodName               ( regex   : /[a-z][\w\s'\(\)]*/ ) // Spock method names
    }
}
