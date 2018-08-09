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

import bsh.BshConsoleInterpreter;
import bsh.ConsoleInterface;
import bsh.InterpreterEvent;
import static bsh.InterpreterEvent.BUSY;
import static bsh.InterpreterEvent.READY;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.jline.utils.AttributedString;
import org.jline.utils.Status;
import ste.beanshell.jline.BshLineReader;

/**
 *
 */
public class JLineConsoleInterface implements ConsoleInterface {

    public static final String DEFAULT_PROMPT = "% ";

    public BshLineReader lineReader = null;
    public PipedWriter pipe = null;

    private Reader in = null;

    public JLineConsoleInterface(BshLineReader reader) throws IOException {
        this.lineReader = reader;
        this.pipe = new PipedWriter();
        this.in   = new PipedReader(pipe);

        this.lineReader.setPrompt(DEFAULT_PROMPT);
    }

    public void on(InterpreterEvent e) {
        if (READY.equals(e.type)) {
            status(READY);
            lineReader.setPrompt(getBshPrompt(e.source));
            lineReader.redisplay();
        } else if (BUSY.equals(e.type)) {
            status(BUSY);
        }
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

    private String getBshPrompt(BshConsoleInterpreter bsh) {
        try {
            return (String)bsh.eval("getBshPrompt()");
        } catch ( Exception e ) {
            return "bsh % ";
        }

    }

    private void status(String msg) {
        Status status = Status.getStatus(lineReader.getTerminal());
        List<AttributedString> lines = new ArrayList<>();
        lines.add(
            new AttributedString(
                new String(new char[lineReader.getTerminal().getWidth()]).replace("\0", "-")
            )
        );
        lines.add(
            new AttributedString(msg)
        );
        //AttributedStyle.INVERSE.foreground(Integer.parseInt("001b", 16)).background(AttributedStyle.WHITE)
        status.update(lines);
        status.redraw();
    }

}
