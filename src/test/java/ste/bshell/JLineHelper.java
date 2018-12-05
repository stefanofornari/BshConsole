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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.BDDAssertions.then;
import org.jline.reader.EndOfFileException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import ste.bshell.jline.EofPipedInputStream;
import ste.bshell.jline.TestLineReader;

/**
 *
 */
public class JLineHelper {

    public TestLineReader givenReader() throws Exception {
        return givenReaderWith(new EofPipedInputStream());
    }

    public TestLineReader givenReaderWith(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = new DumbTerminal("terminal", "ansi", in, out, StandardCharsets.UTF_8);
        terminal.setSize(new Size(80, 25));
        TestLineReader reader = (in instanceof EofPipedInputStream)
                              ? new TestLineReader(terminal, "JLine", null, in)
                              : new TestLineReader(terminal, "JLine", null, new EofPipedInputStream(in)
                              );
        reader.setKeyMap(LineReaderImpl.EMACS);

        return reader;
    }

    public void thenBufferIs(final TestLineReader reader, final String expected) throws IOException {
        reader.getHistory().purge();

        reader.list = false;
        reader.menu = false;

        // run it through the reader
        try {
            while (reader.readLine(null, null, (Character)null, null) != null) {
                // nop
            }
        } catch(EndOfFileException x) { /* nop */ }

        then(reader.getBuffer().toString()).isEqualTo(expected);
    }

    public void thenLinesAre(final TestLineReader reader, final String expected) throws IOException {
        reader.getHistory().purge();

        reader.list = false;
        reader.menu = false;

        // run it through the reader
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine(null, null, (Character)null, null)) != null) {
                sb.append(line).append('\n');
            }
        } catch(EndOfFileException x) { /* nop */ }

        then(sb.toString()).isEqualTo(expected);
    }

}
