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

    @Test(timeout = 1000)
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
        final JLineConsole jline = (JLineConsole)bsh.console;
        then(jline.lineReader.getVariable(LineReader.HISTORY_FILE)).isEqualTo(HISTORY);
    }

    @Test(timeout=1000)
    public void prompt_at_start() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");
        bsh.consoleInit();

        final Thread T = new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }); T.start();

        thenBshIsReady(bsh);

        bsh.close(); T.interrupt();
    }

    /**
     * Interrupt during parsing
     */
    @Test
    public void discard_parsed_input_on_invalid() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");
        bsh.consoleInit();

        final JLineConsole JLINE1 = (JLineConsole)bsh.console;

        final Thread T = new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }); T.setDaemon(true); T.start();

        thenBshIsReady(bsh);

        JLINE1.pipe.write("class A {\n"); JLINE1.pipe.flush();

        JLINE1.lineReader.getTerminal().raise(Terminal.Signal.INT); // ^C
        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return bsh.console != JLINE1;
            }
        });
        final JLineConsole JLINE2 = bsh.getConsole();

        JLINE2.pipe.write("print(\"__done__\");"); JLINE2.pipe.flush();

        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return STDOUT.getLog().contains("__done__");
            }
        });

        then(STDERR.getLog()).doesNotContain("Parser Error");

        bsh.close(); T.interrupt();
    }

    /**
     * Interrupt during tokenization
     */
    @Test
    public void discard_parsed_input_on_tokenizing() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");
        bsh.consoleInit();

        final JLineConsole JLINE1 = (JLineConsole)bsh.console;

        final Thread T = new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }); T.start();

        thenBshIsReady(bsh);

        JLINE1.pipe.write("print(\"this string does not continue...\n"); JLINE1.pipe.flush();

        JLINE1.lineReader.getTerminal().raise(Terminal.Signal.INT); // ^C
        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return bsh.console != JLINE1;
            }
        });
        final JLineConsole JLINE2 = (JLineConsole)bsh.console;

        JLINE2.pipe.write("print(\"__done__\");"); JLINE2.pipe.flush();

        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return STDOUT.getLog().contains("__done__");
            }
        });

        then(STDERR.getLog()).doesNotContain("Lexical error");

        bsh.close(); T.interrupt();
    }

    @Test(timeout = 2000)
    public void interrupt_long_running_task() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.consoleInit();
        JLineConsole jline = bsh.getConsole();

        bsh.eval("getBshPrompt() { return \"\"; };");

        final Thread T = new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }); T.start();

        jline.pipe.write("Thread.sleep(5000);\n"); jline.pipe.flush();
        new WaitFor(500, new Condition() {
            @Override
            public boolean check() {
                return (bsh.will != null);
            }
        });
        final Future WILL = bsh.will;

        //
        // interrupting the execution shall keep the same console interface
        //
        jline.lineReader.getTerminal().raise(Terminal.Signal.INT); // ^C
        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return  WILL.isCancelled();
            }
        });

        then(jline).isSameAs(jline);

        bsh.close(); T.interrupt();
    }

    @Test(timeout = 5000)
    public void send_task_in_background() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("getBshPrompt() { return \"abc> \"; };");
        bsh.consoleInit();

        final Thread T = new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }); T.start();
        final JLineConsole jline = (JLineConsole)bsh.console;

        jline.pipe.write("Thread.sleep(5000);\n"); jline.pipe.flush();
        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return (bsh.will != null);
            }
        });
        final JLineConsole JLINE = jline;
        final Future WILL = bsh.will;

        //
        // interrupting the execution shall keep the same console interface
        //
        Thread.sleep(100);
        jline.lineReader.getTerminal().raise(Terminal.Signal.TSTP);  // ^Z
        Thread.sleep(100);

        //
        // The task should now be suspended, let's issue anotehr command and
        // make sure it executes...
        //
        jline.pipe.write("s = \"hello\";"); jline.pipe.flush();
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

        bsh.close(); T.interrupt();
    }

    @Test
    public void default_mapping_feedback() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        then(PrivateAccess.getStaticValue(BshClassPath.class, "mappingFeedbackListener"))
             .isNotNull().isInstanceOf(EmptyMappingFeedback.class);
    }

    @Test
    public void show_parser_errors() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.consoleInit();
        JLineConsole jline = bsh.getConsole();

        bsh.eval("getBshPrompt() { return \"\"; };");

        final Thread T = new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }); T.start();

        jline.pipe.write("class {\n"); jline.pipe.flush();
        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return STDERR.getLog().contains("Parser Error:");
            }
        });

        bsh.close(); T.interrupt();
    }

    // --------------------------------------------------------- private methods

    private void thenBshIsReady(final BshConsoleInterpreter bsh) {
        new WaitFor(500, new Condition() {
            @Override
            public boolean check() {
                final JLineConsole jline = (JLineConsole)bsh.console;
                return (jline != null) && jline.lineReader.getPrompt().toString().equals("abc> ");
            }
        });
    }
}
