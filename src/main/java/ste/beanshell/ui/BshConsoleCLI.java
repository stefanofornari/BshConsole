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

import bsh.ConsoleInterface;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Reader;

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

import ste.beanshell.BshCompleter;
import ste.beanshell.BshSession;
/**
 *
 * @author ste
 */
public class BshConsoleCLI {

    public static final String OPT_HELP = "--help";

    public static final String VAR_HISTORY_FILE = "HISTORY_FILE";

    public static final BshSession sess = BshSession.init();

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
        //
        // read an internal init script from the resources
        //
        sess.bsh.eval(new InputStreamReader(getClass().getResourceAsStream("/init.bsh")));

        //
        // if provided, read a customization init script
        //
        if (options.initScript != null) {
            try {
                sess.bsh.source(options.initScript);
            } catch (FileNotFoundException x) {
                System.out.println("error: invalid initialization script " + x.getMessage());
                return;
            }
        }

        try (Terminal terminal = buildLineReader(sess);
             BshSession close = sess.start()) {
            while (!options.welcomeOnly) {
                String line = null;
                try {
                    line = lineReader.readLine();
                    if (line.isEmpty()) line = ";";
                } catch (UserInterruptException e) {
                    System.out.println("^");
                    sess.restart();
                    continue;
                } catch (EndOfFileException e) {
                    System.out.println("Good bye...");
                    return;
                }

                sess.write(line.getBytes());
            }
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
    protected Terminal buildLineReader(BshSession sess) throws IOException, EvalError {
        Terminal terminal = TerminalBuilder.terminal();

        terminal.puts(Capability.clear_screen);
        terminal.flush();

        lineReader = (LineReaderImpl) LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(new BshCompleter())
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .build();
        sess.bsh.set("bsh.lineReader", lineReader);
        sess.bsh.getNameSpace().importCommands("ste.beanshell.commands");

        String historyFile = (String) sess.bsh.get(VAR_HISTORY_FILE);
        if (historyFile != null) {
            lineReader.setVariable(
                LineReader.HISTORY_FILE,
                new File(historyFile)
            );
        }
        return terminal;
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
