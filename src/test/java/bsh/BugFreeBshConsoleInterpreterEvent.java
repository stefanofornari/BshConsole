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
package bsh;

import static bsh.InterpreterEvent.BUSY;
import static bsh.InterpreterEvent.DONE;
import static bsh.InterpreterEvent.READY;
import java.util.ArrayList;
import java.util.concurrent.Future;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.beanshell.JLineConsole;
import ste.xtest.concurrent.Condition;
import ste.xtest.concurrent.WaitFor;


/**
 *
 */
public class BugFreeBshConsoleInterpreterEvent {

    @Test
    public void ready_status() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.consoleInit();

        final ArrayList<InterpreterEvent> events = new ArrayList<>();
        bsh.jline = new JLineConsole(bsh.jline.lineReader) {
            @Override
            public void on(InterpreterEvent e) {
                events.add(e);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }).start();

        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return (!events.isEmpty() && READY.equals(events.get(0).type));
            }
        });

        then(events.get(0).type).isEqualTo(READY);
        then(events.get(0).source).isSameAs(bsh);
    }

    @Test
    public void busy_status() throws Exception {
        BshConsoleInterpreter bsh = new BshConsoleInterpreter();
        bsh.consoleInit();

        final ArrayList<InterpreterEvent> events = new ArrayList<>();
        bsh.jline = new JLineConsole(bsh.jline.lineReader) {
            @Override
            public void on(InterpreterEvent e) {
                events.add(e);
            }
        };
        bsh.setConsole(bsh.jline);  // TODO: to be removed when BshConsoleInterpreter won't inherit

        new Thread(new Runnable() {
            @Override
            public void run() {
                bsh.consoleStart();
            }
        }).start();

        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return (!events.isEmpty() && READY.equals(events.get(0).type));
            }
        });

        bsh.jline.pipe.write("a=0;\n"); bsh.jline.pipe.flush();

        new WaitFor(2500, new Condition() {
            @Override
            public boolean check() {
                return (events.size() == 4);  // wait for the execution
            }
        });

        then(events.get(0).type).isEqualTo(READY);
        then(events.get(1).type).isEqualTo(BUSY);
        then(events.get(1).data).isNotNull().isInstanceOf(Future.class);
        then(events.get(2).type).isEqualTo(DONE);
        then(events.get(2).data).isSameAs(events.get(1).data);
        then(events.get(3).type).isEqualTo(READY);

        for (int i=0; i<3; ++i) {
            then(events.get(i).source).isSameAs(bsh);
        }
    }

    // --------------------------------------------------------- private methods

}
