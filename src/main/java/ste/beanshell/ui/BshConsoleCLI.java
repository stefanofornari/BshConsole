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

import bsh.BshConsoleInterpreter;
import bsh.EvalError;
import bsh.TargetError;
import java.io.IOException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 *

 */
public class BshConsoleCLI {

    public static final String OPT_HELP = "--help";

    public static final String VAR_HISTORY_FILE = "HISTORY_FILE";

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

        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        //
        // if provided, read a customization init script
        //
        if (options.initScript != null) {
            try {
                bsh.eval("source(\"" + options.initScript + "\");");
            } catch (TargetError x) {
                String msg = x.getMessage();
                if ((x.getCause() != null) && (x.getCause() instanceof IOException)) {
                    msg = x.getCause().getMessage();
                }
                System.out.println("error: invalid initialization script " +msg);
                return;
            }
        }

        if (options.welcomeOnly) {
            return;
        }

        bsh.consoleInit();
        bsh.consoleStart();
    }

    public void syntax() {
        System.out.println("Usage: " + getClass().getName());
    }

    public static void main(String... args) throws Exception {
        new BshConsoleCLI().launch(args);
    }

    // ------------------------------------------------------- protected methods



    // --------------------------------------------------------- private methods

    // --------------------------------------------------------- PipedInterpeter

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
