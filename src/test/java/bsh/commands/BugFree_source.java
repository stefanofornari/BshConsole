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
import bsh.EvalError;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.core.util.Files;
import org.junit.Test;

/**
 *
 */
public class BugFree_source {

    @Test
    public void source_with_invalid_file() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        try {
            bsh.eval("source(\"notexistingfile.bsh\");");
        } catch (EvalError x) {
            then(x).hasCauseInstanceOf(FileNotFoundException.class);
            then(x.getCause()).hasMessage(new File("notexistingfile.bsh").getAbsolutePath() + " (No such file or directory)");
        }

        File unredeable = Files.newTemporaryFile();
        unredeable.createNewFile();
        unredeable.setReadable(false);
        unredeable.deleteOnExit();

        try {
            bsh.eval("source(\"" + unredeable.getAbsolutePath() + "\");");
        }  catch (EvalError x) {
            then(x).hasCauseInstanceOf(IOException.class);
            then(x.getCause()).hasMessage(unredeable.getAbsolutePath() + " (Permission denied)");
        }
    }

    @Test
    public void source_a_file() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        Object ret = bsh.eval("source(\"src/test/scripts/source.bsh\");");

        then((String)ret).isEqualTo("RETURN VALUE");
        then((Boolean)bsh.get("SOURCED")).isTrue();
    }

    @Test
    public void source_a_url() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        Object ret = bsh.eval(
            "file = new java.io.File(\"src/test/scripts/source.bsh\");" +
            "source(file.toURI().toURL());"
        );

        then((String)ret).isEqualTo("RETURN VALUE");
        then((Boolean)bsh.get("SOURCED")).isTrue();
    }
}
