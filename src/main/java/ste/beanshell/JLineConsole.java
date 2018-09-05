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
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;
import ste.beanshell.jline.BshLineReader;

/**
 *
 */
public class JLineConsole implements ConsoleInterface {

    public static final String DEFAULT_PROMPT = "% ";

    public BshLineReader lineReader = null;
    public PipedWriter pipe = null;

    private Reader in = null;
    private List<Future> tasks = new ArrayList<>();

    public JLineConsole(BshLineReader reader) throws IOException {
        this.lineReader = reader;
        this.pipe = new PipedWriter();
        this.in   = new PipedReader(pipe);

        this.lineReader.setPrompt(DEFAULT_PROMPT);
    }

    public void on(InterpreterEvent e) {
        if (READY.equals(e.type)) {
            status(READY);
            lineReader.setPrompt((String)e.data);
            lineReader.redisplay();
        } else if (BUSY.equals(e.type)) {
            tasks.add((Future)e.data);
            status(BUSY);
        } else if (DONE.equals(e.type)) {
            tasks.remove((Future)e.data);
            status(READY);
        }
    }

    private void status(String msg) {
        Status status = Status.getStatus(lineReader.getTerminal());
        List<AttributedString> lines = new ArrayList<>();

        int width = lineReader.getTerminal().getWidth();

        lines.add(
            new AttributedString(
                new String(new char[width]).replace("\0", "-")
            )
        );

        String statusLine = new String(msg + "\t| ");
        StringBuilder tasksString = new StringBuilder();
        for(Future f: tasks) {
            if (!f.isDone()) {
                tasksString.append(" T" + f.hashCode() + " ");
            }
        }

        lines.add(
            new AttributedString(
                statusLine +
                new AttributedString(
                    tasksString.toString(),
                    AttributedStyle.INVERSE.foreground(2).background(AttributedStyle.WHITE)
                ).toAnsi()
            ).columnSubSequence(0, width)
        );
        status.update(lines);
        status.redraw();
    }

    // -------------------------------------------------------- ConsoleInterface

    @Override
    public Reader getIn() {
        return in;
    }

    @Override
    public PrintStream getOut() {
        return System.out;
    }

    @Override
    public PrintStream getErr() {
        return System.err;
    }

    @Override
    public void println(Object o) {
        System.out.println(o); System.out.flush();
    }

    @Override
    public void print(Object o) {
        System.out.print(o); System.out.flush();
    }

    @Override
    public void error(Object o) {
       System.err.println(o);
    }

    // --------------------------------------------------------- private methods

}
