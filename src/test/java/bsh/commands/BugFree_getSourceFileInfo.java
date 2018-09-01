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
import bsh.Utils;
import java.io.File;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;

/**
 * This is the default getSourceFileInfo()...
 */
public class BugFree_getSourceFileInfo {
    @Test
    public void source_info() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.eval("debug()");

        File f = new File("src/test/scripts/source_file_info.bsh");
        then(
            Utils.source(bsh, f, bsh.getNameSpace())
        ).isEqualTo(f.getAbsolutePath());
    }
}
