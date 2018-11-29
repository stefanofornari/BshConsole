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
package bsh.commands;

import bsh.BshConsoleInterpreter;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * TODO: commands should be alphabetically ordered
 */
public class help {

    public static final String HELP = "bsh.help";

    public static void invoke(final Interpreter bsh, CallStack callstack)
    throws EvalError {
        if (bsh.get(HELP) == null) {
            throw new EvalError("please set bsh.help to an existing and readable directory", null, callstack);
        }

        final String help = String.valueOf(bsh.get(HELP));
        final Path helpPath = Paths.get(help);

        if (!Files.exists(helpPath) || !Files.isDirectory(helpPath)) {
            throw new EvalError("invalid help directory " + help + "; please set bsh.help to an existing and readable directory", null, callstack);
        }

        try {
            try (Stream<Path> paths = Files.walk(helpPath)) {
                paths
                    .filter(Files::isRegularFile)
                    .filter(new Predicate<Path>() {
                        @Override
                        public boolean test(Path p) {
                            return p.toString().endsWith(".txt");
                        }
                    }).forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path p) {
                            String name = StringUtils.removeEnd(
                                              StringUtils.removeStart(p.toString(), help),
                                              ".txt"
                                          ).substring(1).replaceAll("/", ".");
                            bsh.println("");
                            bsh.println(name);
                            bsh.println(StringUtils.repeat("=", name.length()));
                            bsh.println("");
                            try {
                                //
                                // TODO: indent the content of the file
                                //
                                bsh.println(FileUtils.readFileToString(p.toFile(), "UTF8"));
                            } catch (IOException x) {
                                //
                                // TODO: handling
                                //
                            }
                            bsh.println(""); bsh.println("");
                        }
                    });
            }
        } catch (IOException x) {
            //
            // TODO handle the error
            //
        }

        return;
    }
}
