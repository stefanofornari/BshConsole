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
import java.io.ByteArrayInputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.jline.reader.Completer;
import org.junit.Before;
import org.junit.Test;
import ste.beanshell.jline.TestBuffer;
import ste.beanshell.jline.TestLineReader;

/**
 *
 */
public class BugFreeBshCompleter {

    @Before
    public void before() throws Exception {
        Handler ch = new ConsoleHandler();
        ch.setLevel(Level.FINEST);
        Logger logger = Logger.getLogger("org.jline");
        logger.addHandler(ch);
        // Set the handler log level
        logger.setLevel(Level.INFO);

    }

    @Test
    public void construction() throws Exception {
        final BshConsoleInterpreter BSH = new BshConsoleInterpreter();
        then(new BshCompleter(BSH)).isInstanceOf(Completer.class);

        BshCompleter c = new BshCompleter(BSH);
        then(c).hasFieldOrPropertyWithValue("bsh", BSH);

        try {
            new BshCompleter(null);
            fail("missing argument validation");
        } catch (IllegalArgumentException x) {
            then(x).hasMessage("bsh can not be null");
        }
    }

    @Test
    public void the_bsh_list() throws Exception {
        final JLineHelper H = new JLineHelper();
        final BshConsoleInterpreter BSH = new BshConsoleInterpreter();

        BSH.set("myvar", "");
        BSH.eval("myfunc(s){}");

        TestLineReader reader = givenReader(BSH, "System.eq");
        H.thenBufferIs(reader, "System.equals");

        reader = givenReader(BSH, "dummy"); H.thenBufferIs(reader, "dummy");
        reader = givenReader(BSH, "myv"); H.thenBufferIs(reader, "myvar");
        reader = givenReader(BSH, "myfunc"); H.thenBufferIs(reader, "myfunc");
    }

    // --------------------------------------------------------- private methods

    private TestLineReader givenReader(BshConsoleInterpreter bsh, String str)
    throws Exception {
        final JLineHelper H = new JLineHelper();

        TestLineReader reader = H.givenReaderWith(
            new ByteArrayInputStream(new TestBuffer(str).tab().getBytes())
        );

        reader.setCompleter(new BshCompleter(bsh));

        return reader;
    }
}
