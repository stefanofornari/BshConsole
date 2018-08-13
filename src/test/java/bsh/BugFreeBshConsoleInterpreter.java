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

import bsh.classpath.BshClassPath;
import bsh.classpath.EmptyMappingFeedback;
import java.io.File;
import java.util.concurrent.Future;
import static org.assertj.core.api.BDDAssertions.then;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ste.beanshell.JLineConsole;
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

        bsh.consoleInit();

        //
        // I could not find a more reliable way...
        //
        then(bsh.jline.lineReader.getVariable(LineReader.HISTORY_FILE)).isEqualTo(HISTORY);
    }

    @Test(timeout=500)
    public void prompt_at_start() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");
        bsh.consoleInit();

        new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }).start();

        new WaitFor(500, new Condition() {
            @Override
            public boolean check() {
                return (bsh.jline != null) && bsh.jline.lineReader.getPrompt().toString().equals("abc> ");
            }
        });

        System.out.println();
    }

    /**
     * Given that setting the prompt and displaying it are on different threads,
     * we want to make sure we read a line (i.e. display the prompt) only after
     * beanshell is ready to accept input.
     */
    @Test
    public void discard_parsed_input_on_invalid() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");
        bsh.consoleInit();

        new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }).start();
        Thread.sleep(500);

        bsh.jline.pipe.write("class A {\n"); bsh.jline.pipe.flush();

        bsh.jline.lineReader.getTerminal().raise(Terminal.Signal.INT); // ^C
        Thread.sleep(100);

        bsh.jline.pipe.write("print(\"__done__\");"); bsh.jline.pipe.flush();

        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return STDOUT.getLog().contains("__done__");
            }
        });

        then(STDERR.getLog()).doesNotContain("Parser Error");
    }

    @Test(timeout = 1000)
    public void interrupt_long_running_task() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");
        bsh.consoleInit();

        Thread bshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        });
        bshThread.start();

        bsh.jline.pipe.write("Thread.sleep(5000);\n"); bsh.jline.pipe.flush();
        new WaitFor(250, new Condition() {
            @Override
            public boolean check() {
                return (bsh.will != null);
            }
        });
        final JLineConsole JLINE = bsh.jline;
        final Future WILL = bsh.will;

        //
        // interrupting the execution shall keep the same console interface
        //
        bsh.jline.lineReader.getTerminal().raise(Terminal.Signal.INT);  // ^C
        Thread.sleep(100);

        then(bsh.jline).isSameAs(JLINE);
        then(WILL.isCancelled()).isTrue();

        bshThread.interrupt();
    }

    @Test(timeout = 5000)
    public void send_task_in_background() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");
        bsh.consoleInit();

        Thread bshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        });
        bshThread.start();

        bsh.jline.pipe.write("Thread.sleep(5000);\n"); bsh.jline.pipe.flush();
        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return (bsh.will != null);
            }
        });
        final JLineConsole JLINE = bsh.jline;
        final Future WILL = bsh.will;

        //
        // interrupting the execution shall keep the same console interface
        //
        Thread.yield(); Thread.sleep(100);
        bsh.jline.lineReader.getTerminal().raise(Terminal.Signal.TSTP);  // ^Z
        Thread.yield(); Thread.sleep(100);

        //
        // The task should now be suspended, let's issue anotehr command and
        // make sure it executes...
        //
        bsh.jline.pipe.write("s = \"hello\";"); bsh.jline.pipe.flush();
        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                try {
                    return (bsh.get("s") != null);
                } catch (Exception x) {
                    return false;
                }
            }
        });

        then(WILL.isCancelled()).isFalse();

        bshThread.interrupt();
    }

    @Test
    public void default_mapping_feedback() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        then(PrivateAccess.getStaticValue(BshClassPath.class, "mappingFeedbackListener"))
             .isNotNull().isInstanceOf(EmptyMappingFeedback.class);
    }

    // --------------------------------------------------------- private methods
}
