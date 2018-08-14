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

import bsh.InterpreterEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 *
 * @param <T> return value type
 */
public class NodeFuture<T> extends FutureTask<T> {

    final private JLineConsole console;

    public NodeFuture(Callable<T> callable, JLineConsole console) {
        super(callable);
        if (console == null) {
            throw new NullPointerException("console can not be null");
        }
        this.console = console;
    }

    @Override
    protected void done() {
        console.on(new InterpreterEvent("DONE", this));
    }

}
