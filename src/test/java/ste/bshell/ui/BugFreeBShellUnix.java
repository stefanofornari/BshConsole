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
package ste.bshell.ui;

import java.io.File;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ste.xtest.cli.BugFreeCLI;

/**
 *
 */
public class BugFreeBShellUnix extends BugFreeCLI {

    @Rule
    public final TemporaryFolder HOMEDIR = new TemporaryFolder();

    @Before
    public void before() throws Exception {
        FileUtils.copyDirectory(new File("src/main/bshell/bin"), HOMEDIR.newFolder("bin"), true);
        FileUtils.copyDirectory(new File("src/test/bshell/lib"), HOMEDIR.newFolder("lib"), true);

        File launcher = new File(
            new File(HOMEDIR.getRoot(), "bin"), "bshell"
        );

        FileUtils.write(
            launcher,
            FileUtils.readFileToString(launcher).replaceAll("exec ", "#exec ") +
            "\necho $CLASSPATH"
        );

        launcher.setExecutable(true);
    }

    @Test
    public void bshconsole_first_in_classpath() throws Exception {
        final File OUT = HOMEDIR.newFile();

        ProcessBuilder pb =  new ProcessBuilder("bin/bshell");
        pb.directory(HOMEDIR.getRoot());
        pb.redirectOutput(OUT);
        pb.start().waitFor();
        then(FileUtils.readFileToString(OUT))
            .startsWith(HOMEDIR.getRoot().getAbsolutePath() + "/lib/bsh-console-1.0.jar");
    }

    @Test
    public void classpath_constains_CLASSPATH() throws Exception {
        final File OUT = HOMEDIR.newFile();

        ProcessBuilder pb =  new ProcessBuilder("bin/bshell");
        pb.directory(HOMEDIR.getRoot());
        pb.redirectOutput(OUT);

        Map<String, String> env = pb.environment();
        env.put("CLASSPATH", "/some/class/path/a.jar");

        pb.start().waitFor();
        then(FileUtils.readFileToString(OUT))
            .startsWith(HOMEDIR.getRoot().getAbsolutePath() + "/lib/bsh-console-1.0.jar")
            .contains(":/some/class/path/a.jar:");
    }

}
