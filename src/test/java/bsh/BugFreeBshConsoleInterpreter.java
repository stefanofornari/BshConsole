/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bsh;

import java.io.File;
import java.io.PipedWriter;
import java.lang.reflect.Method;
import static org.assertj.core.api.BDDAssertions.then;
import org.jline.reader.LineReader;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ste.xtest.cli.BugFreeCLI;
import ste.xtest.concurrent.Condition;
import ste.xtest.concurrent.WaitFor;
import ste.xtest.reflect.PrivateAccess;


/**
 *
 */
public class BugFreeBshConsoleInterpreter extends BugFreeCLI {
    @Rule
    public final TemporaryFolder ADIR = new TemporaryFolder();

    @Before
    public void before() throws Exception {
        STDOUT.clearLog();
    }

    @Test(timeout = 500)
    public void read_internal_init_scripts_from_resources() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        then(bsh.eval("getBshPrompt();")).isEqualTo("\u001b[34;1mbsh #\u001b[0m ");
    }

    @Test
    public void persistent_history_simple_file() throws Exception {
        final File HISTORY = new File(ADIR.getRoot(), "bshmybsh.history");

        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.set("HISTORY_FILE", HISTORY.getAbsolutePath());
        PrivateAccess.setInstanceValue(bsh, "interactive", false);

        bsh.startConsole();

        //
        // I could not find a more reliable way...
        //
        LineReader lr = (LineReader)PrivateAccess.getInstanceValue(bsh, "lineReader");
        then(lr.getVariable(LineReader.HISTORY_FILE)).isEqualTo(HISTORY);
    }

    @Test(timeout=500)
    public void prompt_at_start() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");

        new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.startConsole();
            }
        }).start();

        new WaitFor(500, new Condition() {
            @Override
            public boolean check() {
                return "abc> ".equals(bsh.prompt);
            }
        });

        System.out.println();
    }

    @Test
    public void prompt_at_start_failure() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { Thread.sleep(10000); }");

        //
        // if bsh does not initialize within 3 secs, show an error prompt
        //
        new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.startConsole();
            }
        }).start();

        new WaitFor(3000, new Condition() {
            @Override
            public boolean check() {
                return STDERR.getLog().contains("Unable to connect to the interpreter... closing.");
            }
        });

        System.out.println();
    }

    @Test
    @Ignore
    public void discard_parsed_input_on_invalid() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");

        new Thread(bsh).start();
        Thread.sleep(500);

        PipedWriter pipe = (PipedWriter)PrivateAccess.getInstanceValue(bsh, "pipe");

        pipe.write("class A {\n"); pipe.flush();

        Method m = bsh.getClass().getDeclaredMethod("reset");
        m.setAccessible(true);
        m.invoke(bsh);

        pipe = (PipedWriter)PrivateAccess.getInstanceValue(bsh, "pipe");
        pipe.write("print(\"__done__\");"); pipe.flush();

        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return STDOUT.getLog().contains("__done__");
            }
        });

        then(STDERR.getLog()).doesNotContain("Parser Error");
    }

    // --------------------------------------------------------- private methods

}
