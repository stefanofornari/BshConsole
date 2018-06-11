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
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.TerminalBuilder;
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
    
    private LineReader lineReader = null;
    
    
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
        
        final PipedOutputStream OUT = new PipedOutputStream();
        final PipedInputStream  IN  = new PipedInputStream(OUT);
        
        Interpreter bsh = new Interpreter(new InputStreamReader(IN), System.out, System.err, true);
        bsh.setExitOnEOF(true);
    
        if (options.initScript != null) {
            try {
                bsh.source(options.initScript);
            } catch (FileNotFoundException x) {
                System.out.println("error: invalid initialization script " + x.getMessage());
                return;
            }
        }
        
        buildLineReader(bsh);
        
        
        
        Thread bshThread = new Thread(bsh);
        bshThread.start();
        
        while (!options.welcomeOnly) {
            String line = null;
            try {
                line = lineReader.readLine();
            } catch (UserInterruptException e) {
                System.out.println("Good bye!");
                IN.close(); OUT.close();
                return;
            } catch (EndOfFileException e) {
                System.out.println("Reached end of file... closing.");
                IN.close(); OUT.close();
                return;
            }
            OUT.write((line.isEmpty()?";":line).getBytes());
            OUT.flush();
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
        lineReader = LineReaderBuilder.builder()
            .terminal(TerminalBuilder.terminal())
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
