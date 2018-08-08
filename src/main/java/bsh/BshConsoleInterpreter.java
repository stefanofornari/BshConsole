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

import static bsh.Interpreter.VERSION;
import bsh.classpath.BshClassPath;
import bsh.classpath.EmptyMappingFeedback;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import ste.beanshell.BshCompleter;
import ste.beanshell.JLineConsoleInterface;
import ste.beanshell.jline.BshLineReader;
import ste.beanshell.jline.BshLineReaderBuilder;
import static ste.beanshell.ui.BshConsoleCLI.VAR_HISTORY_FILE;

/**
 *
 */
public class BshConsoleInterpreter extends Interpreter {

    public boolean DEBUG = false;  // workaround for new bewanshell DEBUG... to be removed
    public JLineConsoleInterface jline;

    protected boolean discard = false;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future will = null;

    static {
        BshClassPath.addMappingFeedback(new EmptyMappingFeedback());
    }

    public BshConsoleInterpreter() throws IOException, EvalError {
        super();

        setShowResults(false);
        setExitOnEOF(false);
        interactive = true;

        //
        // read an internal init script from the resources
        //
        eval(new InputStreamReader(getClass().getResourceAsStream("/init.bsh")));
    }

    public void consoleInit() {
        try {
            jline = new JLineConsoleInterface(buildLineReader());
            setConsole(jline);
        } catch (Exception x) {
            error("Unable to create the line reader... closing.");
            x.printStackTrace();
        }
    }

    public void consoleStart() {
        if (!interactive) {
            //
            // TDOD: error message???
            //
            return;
        }

        Thread bshThread = new Thread(this);
        bshThread.setDaemon(true);
        bshThread.start();

        String line = null;
        while (true) {
            try {
                line = jline.lineReader.readLine();

                if (line.length() == 0) { // special hack for empty return!
                    line += (";\n");
                }

                jline.pipe.write(line); jline.pipe.flush();

                //
                // We reinitialize the prompt to the empty string so that on multi-line
                // inputs no prompt will be displayed. Only when bsh.Interpreter will
                // call getBshPrompt(), the prompt for a new statement will be set.
                //
                jline.lineReader.setPrompt("");
                jline.lineReader.skipRedisplay();
            } catch (UserInterruptException e) {
                cancel();
            } catch (EndOfFileException e) {
                try {
                    jline.pipe.close();
                    executor.shutdown();
                } catch (IOException x) {
                    // nop
                }
                jline.println("See you...");
                return;
            } catch (IOException x) {
                jline.error("IO error... closing.");
                return;
            }
        }
    }

    @Override
    public void run() {
        final BshConsoleInterpreter THIS = this;

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

        int idx = -1;
        while (!Thread.interrupted()) {
            boolean eof = false;
            try {
                console.prompt(getBshPrompt());

                eof = parser.Line();
                if (!discard && (parser.jjtree.nodeArity() > 0)) // number of child nodes
                {
                    final SimpleNode node = (SimpleNode) (parser.jjtree.rootNode());

                    if (DEBUG) {
                        node.dump(">");
                    }

                    Object ret = null;
                    will = executor.submit(new Callable() {
                        @Override
                        public Object call() throws Exception {
                            return node.eval(callstack, THIS);
                        }
                    });

                    try {
                        ret = will.get();
                    } catch (Throwable t) {
                        if (t.getCause() instanceof EvalError) {
                            throw (EvalError)t.getCause();
                        }
                    }

                    // sanity check during development
                    if (callstack.depth() > 1) {
                        throw new InterpreterError(
                                "Callstack growing: " + callstack);
                    }

                    if (ret instanceof ReturnControl) {
                        ret = ((ReturnControl) ret).value;
                    }

                    if( ret != Primitive.VOID ) {
                        setu("$_", ret);
                        setu("$"+(++idx%10), ret);
                        if ( getShowResults() ) {
                            println("--> $" + (idx%10) + " = " + StringUtil.typeValueString(ret));
                        }
                    } else if ( getShowResults() ) {
                        println("--> void");
                    }
                }
            } catch (ParseException e) {
                if (!discard) {
                    error("Parser Error: " + e.getMessage(DEBUG) + " " + parser.jjtree.nodeArity());
                }
                if (DEBUG) {
                    e.printStackTrace();
                }
                parser.reInitInput(console.getIn());
            } catch (InterpreterError e) {
                error("Internal Error: " + e.getMessage());
            } catch (TargetError e) {
                error("Target Exception: " + e.getMessage() );
                if (e.inNativeCode()) {
                    e.printStackTrace(DEBUG, console.getErr());
                }
                setu("$_e", e.getTarget());
            } catch (EvalError e) {
                error( "Evaluation Error: "+e.getMessage() );

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
                discard = false;
                will = null;
            }
        }
    }

    // ------------------------------------------------------- protected methods

    // --------------------------------------------------------- private methods

    /**
     *
     */
    private BshLineReader buildLineReader() throws IOException, EvalError {
        Terminal terminal = TerminalBuilder.terminal();

        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();

        BshLineReader lineReader = BshLineReaderBuilder.builder()
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

        lineReader.setPrompt("");

        return lineReader;
    }

    /**
     * Resets the parser closing the current input stream and creating a new
     * parser.
     */
    private void cancel() {
        if ((will != null) && !will.isDone()) {
            will.cancel(true);
        } else {
            jline.println("\n(...)\n");

            try {
                PipedWriter oldPipe = jline.pipe;
                jline = new JLineConsoleInterface(jline.lineReader);
                console.setIn(jline.getIn());
                parser = new Parser(jline.getIn());
                discard = true;
                oldPipe.close();
            } catch (IOException x) {
                // nothing to do...
                x.printStackTrace();
            }
        }
        jline.lineReader.skipRedisplay();
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
