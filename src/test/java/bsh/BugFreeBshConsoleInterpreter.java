/*
 * Copyright (C) 2018 Stefano Fornari.
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Stefano Fornari.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * STEFANO FORNARI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. STEFANO FORNARI SHALL NOT BE LIABLE FOR ANY
 * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package bsh;

import java.io.File;
import java.lang.reflect.Method;
import static org.assertj.core.api.BDDAssertions.then;
import org.jline.reader.LineReader;
import org.junit.Before;
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
        LineReader lr = (LineReader)PrivateAccess.getInstanceValue(bsh.jline, "lineReader");
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
                return (bsh.jline != null) && "abc> ".equals(bsh.jline.prompt);
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
    public void discard_parsed_input_on_invalid() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");

        new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.startConsole();
            }
        }).start();
        Thread.sleep(500);

        bsh.jline.pipe.write("class A {\n"); bsh.jline.pipe.flush();

        Method m = bsh.getClass().getDeclaredMethod("reset");
        m.setAccessible(true);
        m.invoke(bsh);

        bsh.jline.pipe.write("print(\"__done__\");"); bsh.jline.pipe.flush();

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
