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

import bsh.BshConsoleInterpreter;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.xtest.reflect.PrivateAccess;

/**
 *
 */
public class BugFreeBshNodeExecutor {

    @Test
    public void construction() throws Exception {
        final BshNodeExecutor E = createExecutor();

        then(E).isInstanceOf(ThreadPoolExecutor.class);

        try {
            new BshNodeExecutor(null);
            fail("missing sanity check for console");
        } catch (IllegalArgumentException x) {
            then(x).hasMessage("bsh can not be null");
        }

        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        BshNodeExecutor exec = new BshNodeExecutor(bsh);

        then(PrivateAccess.getInstanceValue(exec, "bsh")).isSameAs(bsh);
    }

    @Test
    public void sanity_check() throws Exception {
        final BshNodeExecutor E = createExecutor();

        try {
            E.newTaskFor(null);
            fail("missing sanity check for callable");
        } catch (IllegalArgumentException x) {
            then(x).hasMessage("callable can not be null");
        }
    }

    @Test
    public void executor_returns_NodeFutures() throws Exception  {
        final BshNodeExecutor E = createExecutor();
        final Callable C = new Callable() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };

        final Future F = E.submit(C);
        then(F).isInstanceOf(NodeFuture.class);
        then(PrivateAccess.getInstanceValue(F, "console")).isNotNull();
    }

    // --------------------------------------------------------- private methods

    private BshNodeExecutor createExecutor() throws Exception {
        final BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.setConsole(new JLineConsole(new JLineHelper().givenReader()));
        return new BshNodeExecutor(bsh);
    }
}

