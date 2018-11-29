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

import bsh.EvalError;
import bsh.Interpreter;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;


/**
 *
 */
public class BugFree_help {

    @Rule
    public final SystemOutRule STDOUT = new SystemOutRule().enableLog();

    @Test
    public void no_help_available() throws Exception {
        final Interpreter bsh = new Interpreter();
        bsh.set("bsh.help", "src/test/bshell/lib");  // no txt here
        help.invoke(bsh, null);
        then(STDOUT.getLog()).isEmpty();
    }

    @Test
    public void no_data_prints_all_command_help() throws Exception {
        final Interpreter bsh = new Interpreter();
        bsh.set("bsh.help", "src/test/bshell/help");
        help.invoke(bsh, null);
        then(STDOUT.getLog())
            .contains("load\n")
            .contains("\n====\n")
            .contains("load(filename)\n")
            .contains("\n--------------")
            .contains("description for load")
            .contains("com.cls\n")
            .contains("\n=======\n")
            .contains("cls()")
            .contains("\n-----")
            .contains("description for cls")
            .contains("com.acme.app.filter\n")
            .contains("\n===================\n")
            .contains("filter(dataset, column, value)")
            .contains("\n------------------------------")
            .contains("description for load");
    }

    @Test
    public void error_if_help_dir_is_not_valid() throws Exception {
        final Interpreter bsh = new Interpreter();

        //
        // not specified
        //
        bsh.set("bsh.help", null);
        try {
            help.invoke(bsh, null);
            fail("missing invalid dir check");
        } catch (EvalError x) {
            then(x).hasMessageStartingWith("please set bsh.help to an existing and readable directory");
        }

        //
        // does not exist
        //
        bsh.set("bsh.help", "/this/does/not/exist");
        try {
            help.invoke(bsh, null);
            fail("missing invalid dir check");
        } catch (EvalError x) {
            then(x).hasMessageStartingWith("invalid help directory /this/does/not/exist; please set bsh.help to an existing and readable directory");
        }

        //
        // not a directory
        //
        bsh.set("bsh.help", "pom.xml");
        try {
            help.invoke(bsh, null);
            fail("missing invalid dir check");
        } catch (EvalError x) {
            then(x).hasMessageStartingWith("invalid help directory pom.xml; please set bsh.help to an existing and readable directory");
        }
    }

}