import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.value.Var;
import org.testfx.framework.junit.ApplicationTest;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BugDemo extends ApplicationTest {

    private CodeArea area = new CodeArea();
    private Vector<Integer> marked = new Vector<>();


    @Test
    public void recreate_bug() {

        moveTo(area);
        area.moveTo(area.getAbsolutePosition(15, area.getParagraphLength(15)));
        press(KeyCode.SPACE);
        sleep(5000);
        area.moveTo(area.getAbsolutePosition(25, area.getParagraphLength(25)));
        moveTo(area);
        sleep(5000);
        press(KeyCode.X);
        sleep(14000);


    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        EventStream<PlainTextChange> stream = area.plainTextChanges().filter(x -> !x.isIdentity());
        stream.subscribe(e -> runHighlighting());
        BorderPane pane = new BorderPane();
        pane.setCenter(area);
        primaryStage.setScene(new Scene(pane, 800, 600));
        primaryStage.centerOnScreen();
        primaryStage.getScene().getStylesheets().add("/HighlightingLight.css");
        primaryStage.show();

        area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        area.replaceText(0, 0, testcontent);

        // If this code is removed, does the issue still occur?
//        EventStream<Optional<Bounds>> caretBounds = EventStreams.nonNullValuesOf(area.caretBoundsProperty());
//
//        Subscription caretPopupSub = EventStreams.combine(caretBounds, Var.newSimpleVar(true).values()).subscribe(tuple3 -> {
//            Optional<Bounds> opt = tuple3._1;
//            if(opt.isPresent()) {
//                Bounds b = opt.get();
//            }
//        });
//        caretPopupSub.and(caretBounds.subscribe(x -> {}));
        marked.add(5);
        marked.add(25);
        marked.add(56);

        area.moveTo(0);
    }

    private void runHighlighting() {
        area.setStyleSpans(0, computHighlighting(area.getText()));
        mappedMarked();

    }

    private void mappedMarked() {

        for (int line : marked) {

            int len = area.getParagraphLength(line);
            StyleSpans<Collection<String>> spans = area.getStyleSpans(line);
            area.setStyleSpans(line, 0, merge(spans, len, "marked"));
        }
    }

    private StyleSpans<Collection<String>> computHighlighting(String text) {

        int lastKwEnd = 0;

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        Matcher matcher = patternCompilerStatic.matcher(text);

        while (matcher.find()) {
            String styleClass =
                    matcher.group("SECTION") != null ? "section" :
                            matcher.group("NUMBERS") != null ? "numbers" :
                                    matcher.group("OPERATORS") != null ? "operators" :
                                            matcher.group("COMMAND") != null ? "command" :
                                                    matcher.group("PAREN") != null ? "paren" :
                                                            matcher.group("BRACKET") != null ? "bracket" :
                                                                    matcher.group("STRING") != null ? "string" :
                                                                            matcher.group("COMMENT") != null ? "comment" :
                                                                                    matcher.group("VARS") != null ? "vars" :
                                                                                            null; /* never happens */
            assert styleClass != null;

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();

        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private static StyleSpans<Collection<String>> merge(StyleSpans<Collection<String>> spans, int lineLength,
                                                        String cssClass) {
        if (spans != null) {
            spans = spans.overlay(
                    StyleSpans.singleton(Collections.singletonList(cssClass), lineLength),
                    (bottomSpan, list) -> {
                        List<String> l = new ArrayList<>(bottomSpan.size() + list.size());
                        l.addAll(bottomSpan);
                        l.addAll(list);
                        return l;
                    });
        }

        return spans;
    }

    private List<String> KEYWORDS = Arrays.asList("set", "if", "stop", "loop", "return", "function", "options", "true", "false", "else", "else if", "trigger", "on", "while", "is");
    private String NUMBERS_PATTERN = "[0-9]";
    private String SECTION_PATTERN = "(?<=\\n)\\s*usage:|executable by:|aliases:|permission:|permission message:|description:|cooldown:|cooldown message:|cooldown bypass:|cooldown storage:";
    private String COMMAND_PATTERN = "(?<=\\G|\\n)command(?=\\s)";
    private String COMMENT_PATTERN = "#[^\n]*";
    private String VAR_PATTERN = "\\{\\S*}";
    private String PAREN_PATTERN = "\\(|\\)";
    private String BRACKET_PATTERN = "\\[|\\]";
    private String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";

    private String joinBoundaryPattern(List<String> items) {
        return "\\b(" + String.join("|", items) + ")\\b";
    }

    private Pattern patternCompilerStatic = Pattern.compile(
            "(?<SECTION>" + SECTION_PATTERN + ")"
                    + "|(?<NUMBERS>" + NUMBERS_PATTERN + ")"
                    + "|(?<OPERATORS>" + joinBoundaryPattern(KEYWORDS) + ")"
                    + "|(?<COMMAND>" + COMMAND_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<VARS>" + VAR_PATTERN + ")");

    private String testcontent = "# do whatever you want with this, just don't post it elsewhere as your own unless you've changed ~80% of the skript\n" +
            "options:\n" +
            "\t# messages\n" +
            "\tprefix: <orange>Login <dark grey>ª<light grey>\n" +
            "\tspecifyapassword: You must specify a password\n" +
            "\tmustregister: You have to register with \"\"/register <password>\"\" before you can login # remember to double quotes\n" +
            "\talreadyloggedin: You are already logged in\n" +
            "\talreadyhavepass: You already have a password\n" +
            "\tpasswordset: Your password has been set\n" +
            "\tincorrectpass: You entered an incorrect password\n" +
            "\thavetologin: Login with \"\"/login <password>\"\" # remember to double quotes\n" +
            "\tnowloggedin: You are now logged in\n" +
            "\tnopassword: Please create a password with \"\"/register <password>\"\" # remember to double quotes\n" +
            "\ttoomanytries: You were kicked for entering an incorrect password too many times\n" +
            "\tnoperm: You do not have permission to use this\n" +
            "\tpasswordreset: Your password has been reset\n" +
            "\tresetmsg: %arg-1%'s password was reset\n" +
            "\t# settings\n" +
            "\tipchange: true # whether or not to log someone out if their ip changes between logins\n" +
            "\tkickfortries: true # whether or not to kick for incorrectly entering a password x amount of times\n" +
            "\tmaxtries: 5 # the amount of tries the player gets to enter the correct password before being kicked, if kickfortries is enabled\n" +
            "\tresetperm: reset.password\n" +
            "\tgravity: false # whether or not the armour stand should be effected by gravity. Potentially abusable to stop fall damage, but I reccomend keeping it on. True = gravity on, false = gravity off\n" +
            "\t# slighty more \"advanced\" options below here\n" +
            "\ttimeout: 0 #The time, in seconds, a user has to be logged off before they have to log in again when they rejoin (set to 0 for none)\n" +
            "\tlist: logindetails #name of the list variable\n" +
            "\t#custom event names\n" +
            "\tevent-register: register\n" +
            "\tevent-login: login\n" +
            "\tevent-incorrect: incorrectpass\n" +
            "\tevent-kick: incorrectkick\n" +
            "\t#probably shouldn't touch these ones honestly\n" +
            "\tsalts: 3\n" +
            "\tpeppers: afjfg\\]nri14753\n" +
            "\talgorithim: SHA-256\n" +
            "#the actual skript\n" +
            "function loginResetAll(i: integer = 0):\n" +
            "\tdelete {{@list}::*}\n" +
            "function loginVehicle(p: player):\n" +
            "\tmake {_p} dismount\n" +
            "\tspawn 1 armor stand at location of {_p}\n" +
            "\tset {_s} to last spawned entity\n" +
            "\tadd \"{Invisible:1}\" to nbt of {_s}\n" +
            "\tif {@gravity} is false:\n" +
            "\t\tadd \"{NoGravity:1}\" to nbt of {_s}\n" +
            "\tmake {_p} ride {_s}\n" +
            "\tset metadata value \"loginStand\" of {_s} to true\n" +
            "function loginstartsWith(s: string, t: string) :: boolean:\n" +
            "\treturn check [subtext of {_s} from characters 1 to length of {_t} is {_t}]\n" +
            "command /resetpass <offline player>:\n" +
            "\ttrigger:\n" +
            "\t\tif player has permission \"{@resetperm}\":\n" +
            "\t\t\tset {_u} to arg-1's uuid\n" +
            "\t\t\tdelete {{@list}::%{_u}%::*}\n" +
            "\t\t\tmessage \"{@prefix} {@resetmsg}\" to player\n" +
            "\t\t\tkick arg-1 due to \"{@prefix} {@passwordreset}\"\n" +
            "\t\telse:\n" +
            "\t\t\tmessage \"{@prefix} {@noperm}\" to player\n" +
            "on command:\n" +
            "\tif command executor is player:\n" +
            "\t\tif {{@list}::%player's uuid%::status} is not set:\n" +
            "\t\t\tcancel the event\n" +
            "\t\t\tmessage \"{@prefix} {@havetologin}\" to player\n" +
            "on packet:\n" +
            "\tif event-string is \"PacketPlayInChat\":\n" +
            "\t\tif {{@list}::%player's uuid%::status} is not set:\n" +
            "\t\t\tcancel the event\n" +
            "\t\tset {_m} to \"%packet field \"\"a\"\"%\"\n" +
            "\t\tif \"%loginstartsWith({_m}, \"\"/login\"\")% %loginstartsWith({_m}, \"\"/login \"\")%\" contains \"true\":\n" +
            "\t\t\tcancel the event\n" +
            "\t\t\tif loginstartsWith({_m}, \"/login \"):\n" +
            "\t\t\t\treplace all \"/login \" in {_m} with \"\"\n" +
            "\t\t\t\tif {{@list}::%player's uuid%::password} is set:\n" +
            "\t\t\t\t\tif {{@list}::%player's uuid%::status} is not set:\n" +
            "\t\t\t\t\t\tloop {{@list}::%player's uuid%::salt::*}:\n" +
            "\t\t\t\t\t\t\tloop split \"{@peppers}\" at \"\":\n" +
            "\t\t\t\t\t\t\t\tif {{@list}::%player's uuid%::password} is hashed \"%loop-value-1%%loop-value-2%%{_m}%\" using \"{@algorithim}\":\n" +
            "\t\t\t\t\t\t\t\t\tset {{@list}::%player's uuid%::status} to true\n" +
            "\t\t\t\t\t\t\t\t\tdelete player's vehicle\n" +
            "\t\t\t\t\t\t\t\t\tdelete {{@list}::%player's uuid%::tries}\n" +
            "\t\t\t\t\t\t\t\t\tsync:\n" +
            "\t\t\t\t\t\t\t\t\t\tset yaw of {{@list}::%player's uuid%::location} to player's yaw\n" +
            "\t\t\t\t\t\t\t\t\t\tset pitch of {{@list}::%player's uuid%::location} to player's pitch\n" +
            "\t\t\t\t\t\t\t\t\t\tteleport player to {{@list}::%player's uuid%::location}\n" +
            "\t\t\t\t\t\t\t\t\tmessage \"{@prefix} {@nowloggedin}\" to player\n" +
            "\t\t\t\t\t\t\t\t\tcall custom event \"{@event-login}\" to details player\n" +
            "\t\t\t\t\t\t\t\t\tstop loop\n" +
            "\t\t\t\t\t\tif {{@list}::%player's uuid%::status} is not set:\n" +
            "\t\t\t\t\t\t\tmessage \"{@prefix} {@incorrectpass}\" to player\n" +
            "\t\t\t\t\t\t\tcall custom event \"{@event-incorrect}\" to details player\n" +
            "\t\t\t\t\t\t\tif {@kickfortries} is true:\n" +
            "\t\t\t\t\t\t\t\tif {{@list}::%player's uuid%::tries} is not set:\n" +
            "\t\t\t\t\t\t\t\t\tset {{@list}::%player's uuid%::tries} to 1\n" +
            "\t\t\t\t\t\t\t\telse:\n" +
            "\t\t\t\t\t\t\t\t\tadd 1 to {{@list}::%player's uuid%::tries}\n" +
            "\t\t\t\t\t\t\t\tif {{@list}::%player's uuid%::tries} is greater than or equal to {@maxtries}:\n" +
            "\t\t\t\t\t\t\t\t\tsync:\n" +
            "\t\t\t\t\t\t\t\t\t\tkick player due to \"{@prefix} {@toomanytries}\"\n" +
            "\t\t\t\t\t\t\t\t\t\tcall custom event \"{@event-kick}\" to details player\n" +
            "\t\t\t\t\telse:\n" +
            "\t\t\t\t\t\tmessage \"{@prefix} {@alreadyloggedin}\"\n" +
            "\t\t\t\telse:\n" +
            "\t\t\t\t\tmessage \"{@prefix} {@mustregister}\"\n" +
            "\t\t\telse:\n" +
            "\t\t\t\tmessage \"{@prefix} {@specifyapassword}\"\n" +
            "\t\telse if \"%loginstartsWith({_m}, \"\"/register\"\")% %loginstartsWith({_m}, \"\"/register \"\")%\" contains \"true\":\n" +
            "\t\t\tcancel the event\n" +
            "\t\t\tif loginstartsWith({_m}, \"/register \"):\n" +
            "\t\t\t\tif {{@list}::%player's uuid%::password} is not set:\n" +
            "\t\t\t\t\treplace all \"/register \" in {_m} with \"\"\n" +
            "\t\t\t\t\tloop {@salts} times:\n" +
            "\t\t\t\t\t\tadd random 20 char string from `a-zA-Z0-9` to {{@list}::%player's uuid%::salt::*}\n" +
            "\t\t\t\t\tset {_ss} to a random element out of {{@list}::%player's uuid%::salt::*}\n" +
            "\t\t\t\t\tset {{@list}::%player's uuid%::password} to hashed \"%{_ss}%%a random element out of split \"\"{@peppers}\"\" at \"\"\"\"%%{_m}%\" using \"{@algorithim}\"\n" +
            "\t\t\t\t\tmessage \"{@prefix} {@passwordset}\"\n" +
            "\t\t\t\t\tmessage \"{@prefix} {@nowloggedin}\"\n" +
            "\t\t\t\t\tset {{@list}::%player's uuid%::status} to true\n" +
            "\t\t\t\t\tdelete player's vehicle\n" +
            "\t\t\t\t\tcall custom event \"{@event-register}\" to details player\n" +
            "\t\t\t\telse:\n" +
            "\t\t\t\t\tmessage \"{@prefix} {@alreadyhavepass}\"\n" +
            "\t\t\telse:\n" +
            "\t\t\t\tmessage \"{@prefix} {@specifyapassword}\"\n" +
            "on quit:\n" +
            "\tdelete {{@list}::%player's uuid%::tries}\n" +
            "\tset {{@list}::%player's uuid%::lastlogin} to now\n" +
            "\tset {{@list}::%player's uuid%::lastip} to hashed player's ip\n" +
            "\tif metadata value \"loginStand\" of player's vehicle is true:\n" +
            "\t\tdelete player's vehicle\n" +
            "on join:\n" +
            "\tif difference between {{@list}::%player's uuid%::lastlogin} and now is greater than or equal to {@timeout} seconds:\n" +
            "\t\tdelete {{@list}::%player's uuid%::status}\n" +
            "\telse if hashed player's ip is not {{@list}::%player's uuid%::lastip}:\n" +
            "\t\tif {@ipchange} is true:\n" +
            "\t\t\tdelete {{@list}::%player's uuid%::status}\n" +
            "\tif {{@list}::%player's uuid%::status} is not set:\n" +
            "\t\tset {{@list}::%player's uuid%::location} to location of player\n" +
            "\t\twait 20 ticks\n" +
            "\t\tloginVehicle(player)\n" +
            "\t\tif {{@list}::%player's uuid%::password} is not set:\n" +
            "\t\t\tmessage \"{@prefix} {@nopassword}\" to player\n" +
            "\t\telse:\n" +
            "\t\t\tmessage \"{@prefix} {@havetologin}\" to player\n" +
            "on click:\n" +
            "\tif {{@list}::%player's uuid%::status} is not set:\n" +
            "\t\tcancel the event\n" +
            "\t\tmessage \"{@prefix} {@havetologin}\" to player\n" +
            "on drop:\n" +
            "\tif {{@list}::%player's uuid%::status} is not set:\n" +
            "\t\tcancel the event\n" +
            "\t\tmessage \"{@prefix} {@havetologin}\" to player\n" +
            "on pick up:\n" +
            "\tif {{@list}::%player's uuid%::status} is not set:\n" +
            "\t\tcancel the event\n" +
            "\t\tmessage \"{@prefix} {@havetologin}\" to player\n" +
            "on damage:\n" +
            "\tif attacker is a player:\n" +
            "\t\tif {{@list}::%attacker's uuid%::status} is not set:\n" +
            "\t\t\tcancel the event\n" +
            "on damage of player:\n" +
            "\tif {{@list}::%victim's uuid%::status} is not set:\n" +
            "\t\tcancel the event\n" +
            "on entity target:\n" +
            "\tif targeted entity is a player:\n" +
            "\t\tif {{@list}::%targeted entity's uuid%::status} is not set:\n" +
            "\t\t\tcancel the event\n" +
            "on packet:\n" +
            "\tif event-string is \"PacketPlayInSteerVehicle\" where [metadata value \"loginStand\" of player's vehicle is true]:\n" +
            "\t\tcancel the event\n";
}