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
package ste.bshell;

import bsh.BshConsoleInterpreter;
import bsh.EvalError;
import java.io.FileReader;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;

/**
 *
 */
public class BugFree_eval {
    @Test
    public void simple_eval() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        bsh.eval("b=2"); then(bsh.get("b")).isEqualTo(2);
        bsh.eval("foo() { return 42; };"); then(bsh.eval("foo()")).isEqualTo(42);
    }

    @Test
    public void eval_error() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        try {
            bsh.eval("bogus();");
            fail("EvalError not thrown");
        } catch (EvalError x) {
            then(x).hasMessageStartingWith("Sourced file: inline evaluation of: ``bogus();'' : Command not found: bogus()");
        }
    }

    @Test
    public void eval_namespaces() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();

        bsh.eval(new FileReader("src/test/scripts/eval.bsh"));

        then(bsh.get("s.b")).isEqualTo(5);
        then(bsh.get("b")).isEqualTo(6);
        then(bsh.get("s.a")).isEqualTo(5);
        then(bsh.get("a")).isEqualTo(6);
    }
}
