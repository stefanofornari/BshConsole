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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.lang.reflect.Method;
import org.apache.commons.io.input.ReaderInputStream;
import static org.assertj.core.api.BDDAssertions.then;
import org.jline.reader.LineReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ste.beanshell.jline.EofPipedInputStream;
import ste.beanshell.jline.TestLineReader;
import ste.xtest.cli.BugFreeCLI;
import ste.xtest.concurrent.Condition;
import ste.xtest.concurrent.WaitFor;


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
     * Given that setting the prompt and displaying it are on different thread,
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

        reset(bsh);

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

    private void reset(BshConsoleInterpreter bsh) throws Exception {
        Method m = bsh.getClass().getDeclaredMethod("reset");
        m.setAccessible(true);
        m.invoke(bsh);
    }

    // --------------------------------------------------------- TempLineReader

    private class TempLineReader extends TestLineReader {
        BshConsoleInterpreter bsh;
        int conut = 0;
        PipedWriter pipeW;
        BufferedReader pipeR;

        public TempLineReader(BshConsoleInterpreter bsh) throws IOException {
            super(bsh.jline.lineReader.getTerminal(), "JLine", null, new EofPipedInputStream());

            this.bsh = bsh;
            this.pipeW = new PipedWriter();
            this.pipeR = new BufferedReader(new PipedReader(this.pipeW));
            this.in.setIn(new ReaderInputStream(pipeR));
            this.prompt = null;
        }

        @Override
        public String readLine(String prompt) {
            try {
                System.out.println(System.currentTimeMillis() +  " - count: " + count);
                bsh.set("done", (++count > 1));
                return pipeR.readLine();
            } catch  (Exception x) {
                return null;
            }
        }
    }

}
