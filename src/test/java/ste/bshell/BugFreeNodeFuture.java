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

import ste.bshell.NodeFuture;
import ste.bshell.JLineConsole;
import bsh.InterpreterEvent;
import java.util.concurrent.Callable;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.bshell.jline.TestLineReader;

/**
 *
 */
public class BugFreeNodeFuture {

    @Test
    public void construction() {
        try {
            new NodeFuture(null, null);
            fail("missing sanity check for callable");
        } catch (NullPointerException x) {
            //
            // for consistency with FutureTask
            //
        }

        try {
            new NodeFuture(createCallable(), null);
            fail("missing sanity check for console");
        } catch (NullPointerException x) {
            then(x).hasMessage("console can not be null");
        }
    }

    @Test
    public void notify_console_when_done() throws Exception {
        final JLineHelper H = new JLineHelper();
        TestLineReader r = H.givenReader();

        final Callable C = createCallable();

        final String[]       type = new String[] { null };
        final NodeFuture[] future = new NodeFuture[] { null };
        NodeFuture f = new NodeFuture(C, new JLineConsole(r) {
            @Override
            public void on(InterpreterEvent e) {
                  type[0] = e.type;
                future[0] = (NodeFuture)e.data;
            }
        });

        f.run(); f.get();
        then(type[0]).isEqualTo("DONE");
        then(future[0]).isNotNull();
    }

     // -------------------------------------------------------- private methods

    private Callable createCallable() {
        return new Callable() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };
    }

}
