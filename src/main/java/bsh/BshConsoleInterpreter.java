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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import ste.beanshell.JLineConsoleInterface;
import static ste.beanshell.ui.BshConsoleCLI.VAR_HISTORY_FILE;

/**
 *
 */
public class BshConsoleInterpreter extends Interpreter {

    public boolean DEBUG = true;  // workaround for new bewanshell DEBUG... to be removed

    protected boolean discard = false;
    protected boolean connected = false;

    JLineConsoleInterface jline;

    static {
        BshClassPath.addMappingFeedback(
            new BshClassPath.MappingFeedback() {
                @Override
                public void startClassMapping() {
                }

                @Override
                public void classMapping(String msg) {
                }

                @Override
                public void errorWhileMapping(String msg) {
                }

                @Override
                public void endClassMapping() {
                }
            }
        );
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

                if (line.length() == 0) {// special hack for empty return!
                    line += (";\n");
                }

                jline.pipe.write(line); jline.pipe.flush();

                //
                // We reinitialize the prompt to the empty string so that on multi-line
                // inputs no prompt will be displayed. Only when bsh.Interpreter will
                // call getBshPrompt(), the prompt for a new statement will be set.
                //
                jline.lineReader.setPrompt("");
            } catch (UserInterruptException e) {
                reset();
            } catch (EndOfFileException e) {
                try {
                    jline.pipe.close();
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
        int idx = -1;
        while (!Thread.interrupted()) {
            boolean eof = false;
            try {
                console.prompt(getBshPrompt());

                eof = parser.Line();
                if (!discard && (parser.jjtree.nodeArity() > 0)) // number of child nodes
                {
                    node = (SimpleNode) (parser.jjtree.rootNode());

                    if (DEBUG) {
                        node.dump(">");
                    }

                    Object ret = node.eval(callstack, this);

                    // sanity check during development
                    if (callstack.depth() > 1) {
                        throw new InterpreterError(
                                "Callstack growing: " + callstack);
                    }

                    if (ret instanceof ReturnControl) {
                        ret = ((ReturnControl) ret).value;
                    }

                    if( ret != Primitive.VOID )
                    {
                        setu("$_", ret);
                        setu("$"+(++idx%10), ret);
                        if ( getShowResults() ) {
                            println("--> $" + (idx%10) + " = " + StringUtil.typeValueString(ret));
                        }
                    } else if ( getShowResults() ) {
                        println("--> void");
                    }

                    if (ret != Primitive.VOID) {
                        setu("$_", ret);
                        setu("$"+(++idx%10), ret);
                    }
                    if (getShowResults()) {
                        println("--> " + ret + " : " + StringUtil.typeString(ret));
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
                discard = false;
            }
        }
    }

    // ------------------------------------------------------- protected methods

    // --------------------------------------------------------- private methods

    /**
     *
     */
    private LineReaderImpl buildLineReader() throws IOException, EvalError {
        Terminal terminal = TerminalBuilder.terminal();

        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();

        LineReaderImpl lineReader = (LineReaderImpl)LineReaderBuilder.builder()
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
    private void reset() {
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
