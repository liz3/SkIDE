function registerSkript() {

    var darkFunc = "#9fceff";

    monaco.editor.defineTheme('skript-light', {
        base: 'vs',
        inherit: true,
        rules: [
        ]
    });
    monaco.editor.defineTheme('skript-dark', {
        base: 'vs-dark',
        inherit: true,
        rules: [
            { token: 'function.call', foreground: darkFunc.substring(1) }
        ]
    });

    monaco.languages.register({
        id: 'skript'
    });
    monaco.languages.setMonarchTokensProvider('skript', {


        keywords: [
            "set", "if", "stop", "loop", "return", "function", "options", "true", "false", "else", "else if", "trigger", "on", "while", "is", "effect", "expression", "command", "delete"
        ],

        typeKeywords: [
            'player', 'integer', 'string', 'text'
        ],

        operators: [
            '=', '>', '<', '!', '~', '?', ':', '==', '<=', '>=', '!=',
            '&&', '||', '++', '--', '+', '-', '*', '/', '&', '|', '^', '%',
            '<<', '>>', '>>>', '+=', '-=', '*=', '/=', '&=', '|=', '^=',
            '%=', '<<=', '>>=', '>>>='
        ],

        // we include these common regular expressions
        symbols: /[=><!~?:&|+\-*\/\^%]+/,

        // C# style strings
        escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,

        // The main tokenizer for our languages
        tokenizer: {
            root: [
                [/\s+[^\}\s]+(?=\(.*\))/, 'function.call'],

                [/[a-z_$][\w$]*/, {
                    cases: {
                        '@typeKeywords': 'keyword',
                        '@keywords': 'keyword',
                        '@default': 'identifier'
                    }
                }],
                [/\{([^{}]|%\{|}%)+}/, 'variable'],


                // whitespace
                {include: '@whitespace'},

                // delimiters and operators
                [/[{}()\[\]]/, '@brackets'],
                [/[<>](?!@symbols)/, '@brackets'],
                [/@symbols/, {
                    cases: {
                        '@operators': 'operator',
                        '@default': ''
                    }
                }],


                // numbers
                [/\d*\.\d+([eE][\-+]?\d+)?/, 'number.float'],
                [/0[xX][0-9a-fA-F]+/, 'number.hex'],
                [/\d+/, 'number'],

                // delimiter: after number because of .\d floats
                [/[;,.]/, 'delimiter'],

                // strings
                [/"([^"\\]|\\.)*$/, 'string.invalid'],  // non-teminated string
                [/"/, {token: 'string.quote', bracket: '@open', next: '@string'}],

                // characters
                [/'[^\\']'/, 'string'],
                [/(')(@escapes)(')/, ['string', 'string.escape', 'string']],
                [/'/, 'string.invalid']
            ],


            string: [
                [/[^\\"]+/, 'string'],
                [/@escapes/, 'string.escape'],
                [/\\./, 'string.escape.invalid'],
                [/"/, {token: 'string.quote', bracket: '@close', next: '@pop'}]
            ],

            whitespace: [
                [/[ \t\r\n]+/, 'white'],
                // [/\/\*/,       'comment', '@comment' ],
                [/#[^]*/, 'comment'],
            ],
        },
    });
    monaco.languages.setLanguageConfiguration('skript', {

        autoClosingPairs: [
            {open: "{", close: "}"},
            {open: "[", close: "]"},
            {open: "%", close: "%"},
            {open: "(", close: ")"},
            {
                open: '"',
                close: '"',
                notIn: ["string"]
            },
            {open: "'", close: "'", notIn: ["string", "comment"]},
            {
                open: "`",
                close: "`",
                notIn: ["string", "comment"]
            }, {open: "/**", close: " */", notIn: ["string"]}],
    });
    monaco.languages.registerCodeActionProvider('skript', {
        provideCodeActions: function (model, range, context, token) {

        }
    });
    monaco.languages.registerCompletionItemProvider('skript', {
        provideCompletionItems: function(model, position, token, context) {
            return {suggestions: skide.autoCompleteRequest(model, position, token, context)};
        }
    });

    monaco.languages.registerLinkProvider("skript", {
    });
    monaco.languages.registerReferenceProvider("skript", {
        provideReferences: function (model, position, context, token) {
            return skide.findReferences(model, position, context);
        }
    });
    monaco.languages.registerDefinitionProvider('skript', {
        provideDefinition: function (model, position, token) {
            var k = Object.keys(token);
            return skide.gotoCall(model, position, (k[0] == "isCancellationRequested" && k[1] == "onCancellationRequested"));
        }
    });

}