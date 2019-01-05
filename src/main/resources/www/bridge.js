window.console = {
    log:function () {
        xlogger.prePrint(0);
        for (var i = 0, j = arguments.length; i < j; i++){
            xlogger.log(arguments[i]);
        }
    } ,
    warn:function () {
        xlogger.prePrint(1);
        for (var i = 0, j = arguments.length; i < j; i++){
            xlogger.warn(arguments[i]);
        }
    },
    error:function () {
        xlogger.prePrint(2);
        for (var i = 0, j = arguments.length; i < j; i++){
            xlogger.error(arguments[i]);
        }
    }
};
var editor = null;
var selection = null;

function getDefaultOptions() {
    return {
        automaticLayout: true,
        mouseWheelScrollSensitivity: 0.1,
        copyWithSyntaxHighlighting: false,
        insertSpaces: false,
        suggestSelection: "recentlyUsed",
        tabCompletion: "true",
        wordBasedSuggestions: false
    };
}
function startEditor(options) {
    editor = monaco.editor.create(document.getElementById('root'), options);
    editor.getAction("editor.action.clipboardCopyAction").run = function() {
        getHook().copy();
    };

    editor.getAction("editor.action.clipboardCutAction").run = function() {
        getHook().cut();
    };
    editor.onDidChangeCursorPosition(function (ev) {
        if (ev == null) {
            getHook().eventNotify("onDidChangeCursorPosition")
        } else {
            getHook().eventNotify("onDidChangeCursorPosition")

        }
    });
    editor.onDidChangeModelContent(function (ev) {
        if (ev == null) {
            getHook().eventNotify("onDidChangeModelContent")
        } else {
            getHook().eventNotify("onDidChangeModelContent")

        }
    });
    editor.onMouseDown(function (ev) {
        getHook().contextMenuEmit(ev);
    });
/*
    editor.addOverlayWidget({
        getDomNode: function () {
            var elem = document.createElement("P");
            elem.innerText = "This is a Cool test";

            return elem;
        },
        getId: function () {
            return "skide-test1";
        },
        getPosition: function () {
            return {
                preference: 2
            }
        }
    });
 */
    return editor;
}

function addCommand(id) {
    editor._commandService.addCommand({
        id: id,
        handler: function () {
            return getHook().commandFire(id);
        }
    })

}

function addCondition(key, keyId) {
    var condition = editor.createContextKey(key, false);
    editor.addCommand(keyId, function () {
        skide.cmdCall(key);
    }, key);

    return condition;
}

function addAction(id, label) {
    return editor.addAction({
        id: id,
        label: label,
        keybindings: [],
        precondition: null,
        keybindingContext: null,
        contextMenuGroupId: 'menu',
        contextMenuOrder: 1.5,
        run: function (ed) {
            getHook().actionFire(id, ed)
        }
    });
}

var getFunc = function () {
    return function () {
    };
};
var getObj = function () {
    return {};
};
var getArr = function () {
    return [];
};
var getHook = function () {
    return skide;
};

function cbhReady() {
    require.config({paths: {'vs': 'lib/vs'}});
    require(['vs/editor/editor.main'], function () {
        registerSkript();
        selection = monaco.Selection;
        cbh.call();
    });
}