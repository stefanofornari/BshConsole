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

import static bsh.Interpreter.DEBUG;
import static bsh.Interpreter.VERSION;
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
import ste.beanshell.BshCompleter;
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

        PipedReader in = (PipedReader) getIn();
        pipe = new PipedWriter();
        in.connect(pipe);

        setShowResults(false);
        setExitOnEOF(false);

        //
        // read an internal init script from the resources
        //
        eval(new InputStreamReader(getClass().getResourceAsStream("/init.bsh")));
    }

    public void startConsole() {
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
        bshThread.setDaemon(true);
        bshThread.start();

        // Initial prompt
        // --------------
        // The parser thread will set the proper prompt calling printPrompt()
        // once ready; let's wait until we get this first printPrompt() before
        // reading the input from the console.
        //
        synchronized (this) {
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
        while (true) {
            try {
                line = lineReader.readLine(prompt);

                if (line.length() == 0) {// special hack for empty return!
                    line += (";\n");
                }

                pipe.write(line);
                pipe.flush();

                //
                // We reinitialize the prompt to the empty string so that on multi-line
                // inputs no prompt will be displayed. Only when bsh.Interpreter will
                // call getBshPrompt(), the prompt for a new statement will be set.
                //
                prompt = "";
            } catch (UserInterruptException e) {
                reset();
            } catch (EndOfFileException e) {
                try {
                    pipe.close();
                } catch (IOException x) {
                    // nop
                }
                System.out.println("See you...");
                return;
            } catch (IOException x) {
                System.out.println("IO error... closing.");
                return;
            }
        }
    }

    @Override
    public void run() {
        if (evalOnly) {
            throw new RuntimeException("bsh Interpreter: No stream");
        }

        /*
          We'll print our banner using eval(String) in order to
          exercise the parser and get the basic expression classes loaded...
          This ameliorates the delay after typing the first statement.
         */
        if (null == getParent()) {
            try {
                eval("printBanner();");
            } catch (EvalError e) {
                println("BeanShell " + VERSION + " - by Pat Niemeyer (pat@pat.net)");
            }
        }

        // init the callstack.
        CallStack callstack = new CallStack(globalNameSpace);

        SimpleNode node = null;
        while (!Thread.interrupted()) {
            boolean eof = false;
            try {
                // try to sync up the console
                System.out.flush();
                System.err.flush();
                Thread.yield();  // this helps a little

                prompt(getBshPrompt());

                eof = parser.Line();
                if (!eof && (parser.jjtree.nodeArity() > 0)) // number of child nodes
                {
                    if (node != null) {
                        node.lastToken.next = null;  // prevent OutOfMemoryError
                    }
                    node = (SimpleNode) (parser.jjtree.rootNode());

                    if (DEBUG) {
                        node.dump(">");
                    }

                    Object ret = node.eval(callstack, this);

                    node.lastToken.next = null;  // prevent OutOfMemoryError

                    // sanity check during development
                    if (callstack.depth() > 1) {
                        throw new InterpreterError(
                                "Callstack growing: " + callstack);
                    }

                    if (ret instanceof ReturnControl) {
                        ret = ((ReturnControl) ret).value;
                    }

                    if (ret != Primitive.VOID) {
                        setu("$_", ret);
                    }
                    if (getShowResults()) {
                        println("--> " + ret + " : " + StringUtil.typeString(ret));
                    }
                }
            } catch (ParseException e) {
                if (!eof) {
                    error("Parser Error: " + e.getMessage(DEBUG));
                }
                if (DEBUG) {
                    e.printStackTrace();
                }

                parser.reInitInput(in);
            } catch (InterpreterError e) {
                error("Internal Error: " + e.getMessage());
            } catch (TargetError e) {
                error("// Uncaught Exception: " + e);
                if (e.inNativeCode()) {
                    e.printStackTrace(DEBUG, err);
                }
                setu("$_e", e.getTarget());
            } catch (EvalError e) {
                error("EvalError: " + e.getMessage());

                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (TokenMgrException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                error("Unknown error: " + e);
                if (DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                parser.jjtree.reset();
                // reinit the callstack
                if (callstack.depth() > 1) {
                    callstack.clear();
                    callstack.push(globalNameSpace);
                }
            }
        }
    }

    // ------------------------------------------------------- protected methods
    // --------------------------------------------------------- private methods

    private void prompt(String prompt) {
        lineReader.setPrompt(this.prompt = prompt);

        //
        // See "Initial prompt"
        //
        synchronized (this) {
            notifyAll();
        }
    }

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

        String historyFile = (String) get(VAR_HISTORY_FILE);
        if (historyFile != null) {
            lineReader.setVariable(
                    LineReader.HISTORY_FILE,
                    new File(historyFile)
            );
        }
    }

    /**
     * Resets the parser closing the current input stream and creating a new
     * parser.
     */
    private void reset() {
        println("(...)");

        try {
            PipedWriter newPipe = new PipedWriter(),
                    oldPipe = pipe;
            this.in = new PipedReader(newPipe);
            parser = new Parser(in);
            pipe = newPipe;
            oldPipe.close();
        } catch (IOException x) {
            // nothing to do...
            x.printStackTrace();
        }
    }

    /**
     * Get the prompt string defined by the getBshPrompt() method in the global
     * namespace. This may be from the getBshPrompt() command or may be defined
     * by the user as with any other method. Defaults to "bsh % " if the method
     * is not defined or there is an error.
     */
    private String getBshPrompt() {
        try {
            return (String) eval("getBshPrompt()");
        } catch (Exception e) {
            return "bsh % ";
        }
    }
}
