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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.jline.keymap.KeyMap;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import static org.jline.reader.LineReader.ACCEPT_LINE;
import static org.jline.reader.LineReader.BACKWARD_CHAR;
import static org.jline.reader.LineReader.BACKWARD_DELETE_CHAR;
import static org.jline.reader.LineReader.BACKWARD_KILL_WORD;
import static org.jline.reader.LineReader.BACKWARD_WORD;
import static org.jline.reader.LineReader.BEGINNING_OF_LINE;
import static org.jline.reader.LineReader.COMPLETE_WORD;
import static org.jline.reader.LineReader.DOWN_HISTORY;
import static org.jline.reader.LineReader.END_OF_LINE;
import static org.jline.reader.LineReader.FORWARD_WORD;
import static org.jline.reader.LineReader.KILL_WORD;
import static org.jline.reader.LineReader.UP_HISTORY;
import static org.jline.reader.LineReader.YANK;
import static org.jline.reader.LineReader.YANK_POP;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author ste
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
        final BshConsoleInterpreter BSH = new BshConsoleInterpreter();

        TestLineReader reader = givenReader();

        reader.setCompleter(new BshCompleter(BSH));

        thenBufferIs(reader, "bsh", new TestBuffer("").tab().tab());
        thenBufferIs(reader, "bsh ", new TestBuffer("b").tab());
        thenBufferIs(reader, "dummy", new TestBuffer("dummy").tab());

        BSH.eval("print(s) { System.out.println(s); }");
        thenBufferIs(reader, "print", new TestBuffer("pr").tab());
    }

    @Test
    public void the_custom_list() throws Exception {
        final BshConsoleInterpreter BSH = new BshConsoleInterpreter();
        TestLineReader reader = givenReader();

        reader.setCompleter(new BshCompleter(BSH));

        BSH.set("COMPLETES", "one, two, three");
        thenBufferIs(reader, "one", new TestBuffer("o").tab());
        thenBufferIs(reader, "two", new TestBuffer("tw").tab());
        thenBufferIs(reader, "four", new TestBuffer("four").tab());
    }

    // --------------------------------------------------------- private methods

    private TestLineReader givenReader() throws Exception {
        EofPipedInputStream in = new EofPipedInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = new DumbTerminal("terminal", "ansi", in, out, StandardCharsets.UTF_8);
        terminal.setSize(new Size(160, 80));
        TestLineReader reader = new TestLineReader(terminal, "JLine", null, in);
        reader.setKeyMap(LineReaderImpl.EMACS);


        return reader;
    }

    private void thenBufferIs(final TestLineReader reader, final String expected, final TestBuffer buffer) throws IOException {
        reader.getHistory().purge();

        reader.list = false;
        reader.menu = false;

        reader.in.setIn(new ByteArrayInputStream(buffer.getBytes()));

        // run it through the reader
        while ((reader.readLine(null, null, (Character)null, null)) != null) {
            // noop
        }

        then(reader.getBuffer().toString()).isEqualTo(expected);
    }


    private String getKeyForAction(final String key) {
        switch (key) {
            case BACKWARD_WORD:        return "\u001Bb";
            case FORWARD_WORD:         return "\u001Bf";
            case BEGINNING_OF_LINE:    return "\033[H";
            case END_OF_LINE:          return "\u0005";
            case KILL_WORD:            return "\u001Bd";
            case BACKWARD_KILL_WORD:   return "\u0017";
            case ACCEPT_LINE:          return "\n";
            case UP_HISTORY:           return "\033[A";
            case DOWN_HISTORY:         return "\033[B";
            case BACKWARD_CHAR:        return "\u0002";
            case COMPLETE_WORD:        return "\011";
            case BACKWARD_DELETE_CHAR: return "\010";
            case YANK:                 return "\u0019";
            case YANK_POP:             return new String(new char[]{27, 121});
            default:
              throw new IllegalArgumentException(key);
        }
    }

    // ---------------------------------------------------------- TestLineReader

    public static class TestLineReader extends LineReaderImpl {
        boolean list = false;
        boolean menu = false;
        EofPipedInputStream in;

        public TestLineReader(Terminal terminal, String appName, Map<String, Object> variables, EofPipedInputStream in) {
            super(terminal, appName, variables);
            this.in = in;
        }

        @Override
        protected boolean doList(List<Candidate> possible, String completed, boolean runLoop, BiFunction<CharSequence, Boolean, CharSequence> escaper) {
            list = true;
            return super.doList(possible, completed, runLoop, escaper);
        }
        @Override
        protected boolean doMenu(List<Candidate> possible, String completed, BiFunction<CharSequence, Boolean, CharSequence> escaper) {
            menu = true;
            return super.doMenu(possible, completed, escaper);
        }
    }

    // ----------------------------------------------------- EofPipedInputStream

    public static class EofPipedInputStream extends InputStream {

        private InputStream in;

        public InputStream getIn() {
            return in;
        }

        public void setIn(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            return in != null ? in.read() : -1;
        }

        @Override
        public int available() throws IOException {
            return in != null ? in.available() : 0;
        }
    }

    // -------------------------------------------------------------- TestBuffer

    protected class TestBuffer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        public TestBuffer() {
            // nothing
        }

        public TestBuffer(String str) {
            append(str);
        }

        public TestBuffer(char[] chars) {
            append(new String(chars));
        }

        @Override
        public String toString() {
            try {
                return out.toString(StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public byte[] getBytes() {
            return out.toByteArray();
        }

        public TestBuffer op(final String op) {
            return append(getKeyForAction(op));
        }

        public TestBuffer ctrlA() {
            return ctrl('A');
        }

        public TestBuffer ctrlD() {
            return ctrl('D');
        }

        /**
         * Generate a CTRL-X sequence where 'X' is the control character
         * you wish to generate.
         * @param let The letter of the control character. Valid values are
         *   'A' through 'Z'.
         * @return The modified buffer.
         */
        public TestBuffer ctrl(char let) {
            return append(KeyMap.ctrl(let));
        }

        public TestBuffer alt(char let) {
            return append(KeyMap.alt(let));
        }
        public TestBuffer enter() {
            return ctrl('J');
        }

        public TestBuffer CR() {
        	return ctrl('M');
        }

        public TestBuffer ctrlU() {
            return ctrl('U');
        }

        public TestBuffer tab() {
            return op(COMPLETE_WORD);
        }

        public TestBuffer escape() {
            return append("\033");
        }

        public TestBuffer back() {
            return op(BACKWARD_DELETE_CHAR);
        }

        public TestBuffer back(int n) {
            for (int i = 0; i < n; i++)
                op(BACKWARD_DELETE_CHAR);
            return this;
        }

        public TestBuffer left() {
            return append("\033[D");
        }

        public TestBuffer left(int n) {
            for (int i = 0; i < n; i++)
                left();
            return this;
        }

        public TestBuffer right() {
            return append("\033[C");
        }

        public TestBuffer right(int n) {
            for (int i = 0; i < n; i++)
                right();
            return this;
        }

        public TestBuffer up() {
            return append(getKeyForAction(UP_HISTORY));
        }

        public TestBuffer down() {
            return append("\033[B");
        }

        public TestBuffer append(final String str) {
            for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
                append(b);
            }
            return this;
        }

        public TestBuffer append(final int i) {
            out.write((byte) i);
            return this;
        }
    }
}
