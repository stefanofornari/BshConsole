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

import bsh.EvalError;
import bsh.Interpreter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import static ste.beanshell.ui.BshConsoleCLI.VAR_HISTORY_FILE;

/**
 *
 */
public class BshConsoleInterpreter extends Interpreter {

    private PipedWriter pipe = new PipedWriter();

    protected LineReaderImpl lineReader = null;
    protected String prompt;

    public BshConsoleInterpreter() throws IOException, EvalError {
        super(
            new PipedReader(),
            System.out,
            System.err,
            true
        );


        PipedReader in = (PipedReader)getIn();
        pipe = new PipedWriter();
        in.connect(pipe);

        setShowResults(false);
        setExitOnEOF(false);

        //
        // read an internal init script from the resources
        //
        eval(new InputStreamReader(getClass().getResourceAsStream("/init.bsh")));
    }

    public PipedWriter reset() throws IOException {
        println("(...)");

        pipe.close();
        pipe = new PipedWriter();

        //resetParser(new PipedReader(pipe));

        return pipe;
    }

    public void start() {
        try {
            buildLineReader();
        } catch (Exception x) {
            error("Unable to create the line reader... closing.");
            x.printStackTrace();
            return;
        }

        if (!interactive) {
            return;
        }

        Thread bshThread = new Thread(this);
        bshThread.start();

        // Initial prompt
        // --------------
        // The parser thread will set the proper prompt calling printPrompt()
        // once ready; let's wait until we get this first printPrompt() before
        // reading the input from the console.
        //
        synchronized(this) {
            try {
                wait(2000);
            } catch (InterruptedException x) {
                // nop
            }
        }
        if (prompt == null) {
            error("Unable to connect to the interpreter... closing.");
            return;
        }

        String line = null;
        while(true) {
            try {
                line = lineReader.readLine(prompt);

                if ( line.length() == 0 )  {// special hack for empty return!
                    line += (";\n");
                }

                pipe.write(line); pipe.flush();

                //
                // We reinitialize the prompt to the empty string so that on multi-line
                // inputs no prompt will be displayed. Only when bsh.Interpreter will
                // call getBshPrompt(), the prompt for a new statement will be set.
                //
                prompt = "";
            } catch (UserInterruptException e) {
                try {
                    pipe = reset();
                } catch (IOException x) {
                    // nop
                }
            } catch (EndOfFileException e) {
                try {
                    pipe.close();
                } catch (IOException x) {
                    // nop
                }
                System.out.println("Reached end of file... closing.");
                return;
            } catch (IOException x) {
                System.out.println("IO error... closing.");
                return;
            }
        }
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

    // ------------------------------------------------------- protected methods

    // --------------------------------------------------------- private methods

    /**
     *
     */
    private void buildLineReader() throws IOException, EvalError {
        Terminal terminal = TerminalBuilder.terminal();

        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();

        lineReader = (LineReaderImpl) LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(new BshCompleter(this))
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .build();

        String historyFile = (String)get(VAR_HISTORY_FILE);
        if (historyFile != null) {
            lineReader.setVariable(
                LineReader.HISTORY_FILE,
                new File(historyFile)
            );
        }
    }
}