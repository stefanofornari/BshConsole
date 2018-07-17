/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package ste.beanshell.jline;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.jline.keymap.KeyMap;
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

public class TestBuffer {

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
     * Generate a CTRL-X sequence where 'X' is the control character you wish to
     * generate.
     *
     * @param let The letter of the control character. Valid values are 'A'
     * through 'Z'.
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
        for (int i = 0; i < n; i++) {
            op(BACKWARD_DELETE_CHAR);
        }
        return this;
    }

    public TestBuffer left() {
        return append("\033[D");
    }

    public TestBuffer left(int n) {
        for (int i = 0; i < n; i++) {
            left();
        }
        return this;
    }

    public TestBuffer right() {
        return append("\033[C");
    }

    public TestBuffer right(int n) {
        for (int i = 0; i < n; i++) {
            right();
        }
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
}
