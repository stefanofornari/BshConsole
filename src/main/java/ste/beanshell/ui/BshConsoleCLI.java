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
package ste.beanshell.ui;

import bsh.EvalError;
import bsh.Interpreter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 *
 * @author ste
 */
public class BshConsoleCLI {
        
    public static final String OPT_HELP = "--help";
    
    public static final String VAR_HISTORY_FILE = "HISTORY_FILE";
    
    private LineReaderImpl lineReader = null;
    
    
    public void launch(String... args) throws IOException, EvalError {
        BshConsoleCLI.BshConsoleOptions options = new BshConsoleCLI.BshConsoleOptions();
        
        CommandLine cli = new CommandLine(options);
            
        try {
            cli.parse(args);
        } catch (ParameterException x) {
            System.out.println("\nInvalid arguments: " + x.getMessage() + "\n");
            cli.usage(System.out);
            return;
        }
        
        if (options.help) {
            cli.usage(System.out);
            return;
        }

        PipedOutputStream OUT = new PipedOutputStream();
        PipedInputStream  IN  = new PipedInputStream(OUT);

        Interpreter bsh = new Interpreter(new InputStreamReader(IN), System.out, System.err, true);
        bsh.setExitOnEOF(false);

        //
        // read an internal init script from the resources
        //
        bsh.eval(new InputStreamReader(getClass().getResourceAsStream("/init.bsh")));
    
        //
        // if provided, read a customization init script
        //
        if (options.initScript != null) {
            try {
                bsh.source(options.initScript);
            } catch (FileNotFoundException x) {
                System.out.println("error: invalid initialization script " + x.getMessage());
                return;
            }
        }
        buildLineReader(bsh);
        
        //
        // provide the scripts a way to access jline lineReader; we will use
        // bsh.Interpreter callback to getBshPrompt to set the correct promopt
        //
        bsh.set("bsh.lineReader", lineReader);
       
        Thread bshThread = new Thread(bsh);
        bshThread.start();
        
        String prompt = (String)bsh.get("bsh.prompt");
        while (!options.welcomeOnly) {
            String line = null;
            try {
                line = lineReader.readLine();
            } catch (UserInterruptException e) {
                IN.close(); OUT.close();
                bshThread.interrupt();
                OUT = new PipedOutputStream();
                IN  = new PipedInputStream(OUT);
                bsh = new Interpreter(new InputStreamReader(IN),
                    System.out, System.err, true,
                    bsh.getNameSpace(), bsh, bsh.getSourceFileInfo());
                bsh.setExitOnEOF(false);
                bshThread = new Thread(bsh);
                bshThread.start();
                continue;
            } catch (EndOfFileException e) {
                System.out.println("Reached end of file... closing.");
                IN.close(); OUT.close();
                return;
            }

            OUT.write(line.isEmpty()?";".getBytes():line.getBytes());
            OUT.flush();
            //
            // We reinitialize the prompt to the empty string so that on multi-line
            // inputs no prompt will be displayed. Only when bsh.Interpreter will
            // call getBshPrompt(), the prompt for a new statement will be set.
            //
            lineReader.setPrompt("");
        }
    }
    
    public void syntax() {
        System.out.println("Usage: " + getClass().getName());
    }
    
    public static void main(String... args) throws Exception {
        new BshConsoleCLI().launch(args);
    }
    
    // ------------------------------------------------------- protected methods
    
    /**
     * 
     * @throws IOException 
     */
    protected void buildLineReader(Interpreter bsh) throws IOException, EvalError {
        Terminal terminal = TerminalBuilder.terminal();

        terminal.puts(Capability.clear_screen);
        terminal.flush();

        lineReader = (LineReaderImpl) LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(new BshCompleter(bsh))
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .build();
        
        String historyFile = (String)bsh.get(VAR_HISTORY_FILE);
        if (historyFile != null) {
            lineReader.setVariable(
                LineReader.HISTORY_FILE,
                new File(historyFile)
            );
        }
    }
    
    // --------------------------------------------------------- private methods
    
    
    // -------------------------------------------------------- CommonParameters
    
    @Command(
        name = "ste.beanshell.ui.BshConsoleCLI", 
        description = "A modern console for BeanShell"
    )
    protected static class BshConsoleOptions {
        @Option(
            names = "--help, -h, help", 
            description = "This help message"
        )
        public boolean help;
        
        @Option(
            names="--init", 
            description = "An initialization script sourced at startup"
        )
        public String initScript;
        
        @Option(
            names="--welcome", 
            description = "Execute the init script if provided and exit"
        )
        public boolean welcomeOnly;
    }
    
}
