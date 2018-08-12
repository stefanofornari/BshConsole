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
package ste.beanshell;

import bsh.ConsoleInterface;
import bsh.InterpreterEvent;
import static bsh.InterpreterEvent.BUSY;
import static bsh.InterpreterEvent.DONE;
import static bsh.InterpreterEvent.READY;
import java.io.ByteArrayOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.Status;
import org.junit.Test;
import ste.beanshell.jline.TestBuffer;
import ste.beanshell.jline.TestLineReader;
import ste.xtest.cli.BugFreeCLI;
import ste.xtest.reflect.PrivateAccess;

/**
 *
 */
public class BugFreeJLineConsole extends BugFreeCLI {

    private final JLineHelper H = new JLineHelper();

    @Test
    public void is_a_ConsoleInterface() throws Exception {
        then(new JLineConsole(H.givenReader())).isInstanceOf(ConsoleInterface.class);
    }

    @Test
    public void constructor() throws Exception {
        TestLineReader r = H.givenReader();
        JLineConsole console = new JLineConsole(r);

        H.thenLinesAre((TestLineReader)console.lineReader, "hello world\n", new TestBuffer("hello world").CR());
    }

    @Test
    public void streams() throws Exception {
        JLineConsole console = new JLineConsole(H.givenReader());

        then(console.pipe).isNotNull().isInstanceOf(PipedWriter.class);
        then(console.getIn()).isNotNull().isInstanceOf(PipedReader.class);
        then(console.getOut()).isSameAs(System.out);
        then(console.getErr()).isSameAs(System.err);
    }

    @Test
    public void print_println_error() throws Exception {
        JLineConsole console = new JLineConsole(H.givenReader());

        console.print("hello "); then(STDOUT.getLog()).isEqualTo("hello ");
        console.print("world"); then(STDOUT.getLog()).isEqualTo("hello world");
        console.println("!"); then(STDOUT.getLog()).isEqualTo("hello world!\n");

        console.error("hello"); then(STDERR.getLog()).isEqualTo("hello\n");
        console.error("world"); then(STDERR.getLog()).isEqualTo("hello\nworld\n");
    }

    @Test
    public void default_prompt() throws Exception {
        JLineConsole console = new JLineConsole(H.givenReader());

        then(console.lineReader.getPrompt().toString()).isEqualTo("% ");
    }

    @Test
    public void event_ready() throws Exception {
        final JLineHelper H = new JLineHelper();
        TestLineReader r = H.givenReader();

        JLineConsole console = new JLineConsole(r);

        console.lineReader.setPrompt("abc> ");
        console.on(new InterpreterEvent(null, READY));
        H.thenBufferIs(r, "abc> ", new TestBuffer("abc> "));

        console.lineReader.setPrompt("cde# ");
        console.on(new InterpreterEvent(null, READY));
        H.thenBufferIs(r, "cde# ", new TestBuffer("cde# "));
    }

    @Test
    public void change_status_on_events() throws Exception {
        final JLineHelper H = new JLineHelper();
        TestLineReader r = H.givenReader();
        Status status = ((DumbTerminal)r.getTerminal()).getStatus();
        PrivateAccess.setInstanceValue(status, "supported", true);
        status.resize();

        ByteArrayOutputStream out = (ByteArrayOutputStream)r.getTerminal().output();

        JLineConsole console = new JLineConsole(r);
        console.on(new InterpreterEvent(null, READY));
        then(out.toString()).contains(StringUtils.repeat('-', 80)).contains("READY");
        out.reset();

        Future f = new CompletableFuture();
        console.on(new InterpreterEvent(null, BUSY, f));
        then(out.toString())
            .contains(StringUtils.repeat('-', 80)).contains("BUSY").contains("T" + f.hashCode());
        out.reset();

        console.on(new InterpreterEvent(null, DONE, f));
        then(out.toString())
            .contains(StringUtils.repeat('-', 80)).doesNotContain("T" + f.hashCode());

    }

}