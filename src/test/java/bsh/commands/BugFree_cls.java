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
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.beanshell.JLineConsole;
import ste.beanshell.JLineHelper;
import ste.beanshell.jline.TestLineReader;

/**
 *
 */
public class BugFree_cls {

    @Test
    public void clear_screen() throws Exception {
        final BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        final JLineHelper H = new JLineHelper();
        final TestLineReader r = H.givenReader();

        final StringBuilder out = new StringBuilder();
        final JLineConsole jline = new JLineConsole(r) {
            @Override
            public void print(Object o) {
                out.append(String.valueOf(o));
            }
        };

        bsh.setConsole(jline);

        cls.invoke(bsh, null);

        then(out.toString()).isEqualTo("\033[2J\033[1;1H");
    }

}
