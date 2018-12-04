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

import static bsh.InterpreterEvent.BUSY;
import static bsh.InterpreterEvent.READY;
import bsh.classpath.BshClassPath;
import bsh.classpath.EmptyMappingFeedback;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import ste.beanshell.BshCompleter;
import ste.beanshell.BshNodeExecutor;
import ste.beanshell.JLineConsole;
import ste.beanshell.NodeFuture;
import ste.beanshell.jline.BshLineReader;
import ste.beanshell.jline.BshLineReaderBuilder;
import static ste.beanshell.ui.BshConsoleCLI.VAR_HISTORY_FILE;

/**
 *
 */
public class BshConsoleInterpreter extends Interpreter implements Runnable {

    private Thread bshThread = null;

    protected boolean waitForTask = true;

    BshNodeExecutor executor = null;
    NodeFuture will = null;

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
        JLineConsole jline = null;
        try {
            jline = new JLineConsole(buildLineReader());
            setConsole(jline);
            jline.lineReader.getTerminal().handle(Terminal.Signal.TSTP, new Terminal.SignalHandler() {
                @Override
                public void handle(Terminal.Signal signal) {
                    waitForTask = false;
                }
            });
        } catch (Exception x) {
            jline.error("Unable to create the line reader... closing.");
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
        executor = new BshNodeExecutor(this);

        bshThread = new Thread(this);
        bshThread.setDaemon(true);
        bshThread.start();

        String line = null;
        JLineConsole jline = null;
        while (!Thread.interrupted() && ((jline = getConsole()) != null)) {
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
            } catch (UserInterruptException x) {
                cancel();
            } catch (EndOfFileException x) {
                jline.println("\n(... see you ...)\n");
                close();
                executor.shutdown(); bshThread.interrupt();

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

        // init the callstack.
        CallStack callstack = new CallStack(globalNameSpace);

        int idx = -1;
        boolean eof = false;
        JLineConsole oldConsole = getConsole();
        while (!Thread.interrupted() && !eof) {
            Parser parser = new Parser(getConsole().getIn());

            getConsole().on(new InterpreterEvent(READY, getBshPrompt()));
            try {
                eof = parser.Line();
                if (!getConsole().isValid() || (parser.jjtree.nodeArity() == 0)) {
                    continue;
                }

                final SimpleNode node = (SimpleNode) (parser.jjtree.rootNode());

                if (DEBUG.get()) {
                    node.dump(">");
                }

                final CallStack CURRENT_CURRENT_STACK = callstack;
                Object ret = null;
                waitForTask = true;
                will = (NodeFuture)executor.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        return node.eval(CURRENT_CURRENT_STACK, THIS);
                    }
                });

                getConsole().on(new InterpreterEvent(BUSY, will));
                boolean inBackground = false;
                while (!will.isDone()) {
                    //
                    // if the is a future running, let's create a new stack
                    // so that in the case it is put in background, we have
                    // a clean call stack
                    //
                    callstack = new CallStack(globalNameSpace);
                    try {
                        ret = will.get(25, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException c) {
                        if (waitForTask) {
                            continue;
                        } else {
                            if (!inBackground) {
                                println("\n(... in background ...)\n");
                                inBackground = true;
                            }
                        }

                    } catch(InterruptedException | CancellationException x) {
                        //
                        // it will break at the end...
                        //
                    } catch (Throwable t) {
                        if (t.getCause() instanceof EvalError) {
                            throw (EvalError)t.getCause();
                        } else {
                            throw new TargetError(t.getCause(), node, callstack);
                        }
                    }
                    break;
                };

                if (waitForTask && !will.isCancelled()) {
                    // sanity check during development
                    if (callstack.depth() > 1) {
                        throw new InterpreterError(
                                "Callstack growing: " + callstack);
                    }

                    if (ret instanceof ReturnControl) {
                        ret = ((ReturnControl) ret).value;
                    }

                    if (interactive) {
                        if( ret != Primitive.VOID ) {
                            setu("$_", ret);
                            setu("$"+(++idx%10), ret);
                            if ( getShowResults() ) {
                                console.println("--> $" + (idx%10) + " = " + StringUtil.typeValueString(ret));
                            }
                        } else if ( getShowResults() ) {
                            console.println("--> void");
                        }
                    }
                }
            } catch (TokenMgrException e) {
                //
                // we do not really need to do anything...
                //
                if (DEBUG.get()) {
                    e.printStackTrace();
                }
            } catch (ParseException e) {
                if (!getConsole().isValid()) {
                    console.error("Parser Error: " + e.getMessage(DEBUG.get()));
                }
                if (DEBUG.get()) {
                    e.printStackTrace();
                }
                parser.reInitInput(console.getIn());
            } catch (InterpreterError e) {
                console.error("Internal Error: " + e.getMessage());
            } catch (TargetError e) {
                console.error("Target Exception: " + e.getMessage() );
                if (e.inNativeCode()) {
                    e.printStackTrace(DEBUG.get(), console.getErr());
                }
                setu("$_e", e.getTarget());
            } catch (EvalError e) {
                console.error( "Evaluation Error: "+e.getMessage() );

                if (DEBUG.get()) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                console.error("Unknown error: " + e);
                if (DEBUG.get()) {
                    e.printStackTrace();
                }
            } finally {
                // reinit the callstack
                if (callstack.depth() > 1) {
                    callstack.clear();
                    callstack.push(globalNameSpace);
                }
                eof = !interactive;
                will = null;
            }
        }

        if ( interactive && exitOnEOF ) {
            System.exit(0);
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (IOException x) {
            //
            // nbothing we can do about it...
            //
        }
        bshThread.interrupt();
    }

    /**
     * Returns the current console attached to the interpreter. Note that console
     * can change quite often, due to user interactions (e.g. ^C). Use getConsole()
     * as a convenient method to get the most current JLineConsole.
     *
     * @return the current console as a JLineConsole object.
     */
    public JLineConsole getConsole() {
        return (JLineConsole)console;
    }

    // ------------------------------------------------------- protected methods

    // --------------------------------------------------------- private methods

    private String getBshPrompt() {
        try {
            return (String)eval("getBshPrompt()");
        } catch ( Exception e ) {
            return "bsh % ";
        }

    }

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
        final JLineConsole jline = getConsole();

        if ((will != null) && !will.isDone()) {
            will.cancel(true);
            console.println("(... aborted ...)\n");
        } else {
            try {
                PipedWriter oldPipe = jline.pipe; jline.pipe = null;
                setConsole(new JLineConsole(jline.lineReader)); // TODO: do we realle need to recreate a new console?
                oldPipe.close();
            } catch (IOException x) {
                // nothing to do...
                x.printStackTrace();
            }
            console.println("\n(... discarded ...)\n");
        }
        jline.lineReader.skipRedisplay();
    }
}
