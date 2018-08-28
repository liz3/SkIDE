var editor = null;
var selection = null;

function getDefaultOptions() {
    return {
        language: 'skript',
        automaticLayout: true,
        mouseWheelScrollSensitivity: 0.1
    };
}

function startEditor(options) {
    editor = monaco.editor.create(document.getElementById('root'), options);
    editor.onDidChangeModel(function (ev) {
        if (ev == null) {
            getHook().eventNotify("onDidChangeModel", {})
        } else {
            getHook().eventNotify("onDidChangeModel", ev)

        }
    });
    editor.onDidChangeModelContent(function (ev) {
        console.log(ev);
        if (ev == null) {
            getHook().eventNotify("onDidChangeModelContent", {})
        } else {
            getHook().eventNotify("onDidChangeModelContent", ev)

        }
    });
    editor.onMouseDown(function (ev) {
        getHook().contextMenuEmit(ev);
    });
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
        keybindings: [
            /*
               monaco.KeyMod.CtrlCmd | monaco.KeyCode.F10,
               monaco.KeyMod.chord(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KEY_K, monaco.KeyMod.CtrlCmd | monaco.KeyCode.KEY_M)
             */
        ],
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