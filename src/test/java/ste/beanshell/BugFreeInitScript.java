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

import bsh.Interpreter;
import java.io.InputStreamReader;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;

/**
 *
 */
public class BugFreeInitScript {
    
    @Test
    public void getBshPrompt_returns_empty_string() throws Exception {
        Interpreter bsh = givenInterpreterWithInitScript();
        
        bsh.eval("bsh.prompt=\"HELLO\"; p = getBshPrompt();");
        then((String)bsh.get("p")).isEqualTo("");
        bsh.eval("bsh.prompt=\"HELLO2\"; p = getBshPrompt();");
        then((String)bsh.get("p")).isEqualTo("");
    }
    
    @Test
    public void getBshPrompt_assigns_line_reader_prompt() throws Exception {
        Interpreter bsh = givenInterpreterWithInitScript();
        
        bsh.eval("bsh.prompt=\"HELLO\"; p = getBshPrompt();");
        then((String)bsh.get("bsh.lineReader.prompt")).isEqualTo("HELLO");
        
        bsh.eval("bsh.prompt=\"HELLO2\"; p = getBshPrompt();");
        then((String)bsh.get("bsh.lineReader.prompt")).isEqualTo("HELLO2");
    }
    
    
    // --------------------------------------------------------- private methods
    
    private Interpreter givenInterpreterWithInitScript() throws Exception {
        Interpreter bsh = new Interpreter();
        
        bsh.eval(new InputStreamReader(getClass().getResourceAsStream("/init.bsh")));
        
        return bsh;
    }

    
}
