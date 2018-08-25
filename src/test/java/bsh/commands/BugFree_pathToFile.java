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
import java.io.File;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;

/**
 *
 */
public class BugFree_pathToFile {

    @Test
    public void bsolute_path_from_relative_filename() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.set("bsh.cwd", ".");

        then(((File)bsh.eval("pathToFile(\"a/path/with/file.txt\")")).getAbsolutePath())
            .isEqualTo(new File("a/path/with/file.txt").getAbsolutePath());

        bsh.set("bsh.cwd", "parent");
        then(((File)bsh.eval("pathToFile(\"a/path/with/file.txt\")")).getAbsolutePath())
            .isEqualTo(new File("parent/a/path/with/file.txt").getAbsolutePath());

        bsh.set("bsh.cwd", "/tmp");
        then(((File)bsh.eval("pathToFile(\"a/path/with/file.txt\")")).getAbsolutePath())
            .isEqualTo("/tmp/a/path/with/file.txt");
    }

    @Test
    public void absolute_path_from_absolute_path() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.set("bsh.cwd", "parent");

        then(((File)bsh.eval("pathToFile(\"/a/path/with/file.txt\")")).getAbsolutePath())
            .isEqualTo(new File("/a/path/with/file.txt").getAbsolutePath());
    }
}
