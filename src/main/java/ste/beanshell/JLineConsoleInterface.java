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
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.Reader;
import org.jline.reader.impl.LineReaderImpl;

/**
 *
 */
public class JLineConsoleInterface implements ConsoleInterface {

    public LineReaderImpl lineReader = null;
    public PipedWriter pipe = null;
    public String prompt = null;

    private Reader in = null;

    public JLineConsoleInterface(LineReaderImpl reader) throws IOException {
        this.lineReader = reader;
        this.pipe = new PipedWriter();

        this.in  = new PipedReader(pipe);
    }

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

    @Override
    public void prompt(String prompt) {
        lineReader.setPrompt(this.prompt = prompt);

        //
        // See "Initial prompt"
        //
        synchronized (this) {
            notifyAll();
        }
    }

    // --------------------------------------------------------- private methods

}