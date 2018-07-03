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
package ste.beanshell;

import java.io.InputStreamReader;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;

/**
 *
 */
public class BugFreeInitScript {

    @Test
    public void getBshPrompt_returns_empty_string() throws Exception {
        BshConsoleInterpreter bsh = givenInterpreterWithInitScript();

        bsh.eval("p = getBshPrompt();");
        then((String)bsh.get("p")).isEqualTo("\u001b[34;1mbsh #\u001b[0m ");
        bsh.eval("getBshPrompt() { return \"HELLO2\"; }; p = getBshPrompt();");
        then((String)bsh.get("p")).isEqualTo("HELLO2");
    }

    // --------------------------------------------------------- private methods

    private BshConsoleInterpreter givenInterpreterWithInitScript() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        bsh.eval(new InputStreamReader(getClass().getResourceAsStream("/init.bsh")));

        return bsh;
    }


}
