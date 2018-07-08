package ste.beanshell;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Reader;

import bsh.ConsoleInterface;
import bsh.Interpreter;


public class BshSession implements ConsoleInterface, AutoCloseable {
    private static final BshSession instance = new BshSession();
    private Thread t;
    private PipedOutputStream OUT;
    public Interpreter bsh;

    /** Private constructor for singleton.
     * Instantiated by static member instance.*/
    private BshSession() {
        bsh = new Interpreter(this);
    }

    /** Return the singleton reference.
     * @return BshSession singleton reference. */
    public static BshSession init() {
        return instance;
    }

    /** Start a new interpreter thread.
     * @return BshSession singleton reference. */
    public BshSession start() {
        ( t = new Thread(bsh) ).start();
        return instance;
    }

    /** Restart interpreter and thread.
     * @return BshSession singleton reference. */
    public BshSession restart() {
        bsh.setExitOnEOF(false);
        bsh.setShowResults(false);
        bsh.setYieldDelay(1000);
        this.close();
        bsh = new Interpreter(this, bsh);
        return this.start();
    }

    /** Delegate method for OUT.write().
     * @param b byte array data to write. */
    public void write(byte b[]) {
        try {
            this.OUT.write(b);
            this.OUT.flush();
        } catch (IOException e) {
            System.err.println("BshSession Exception in write: " + e);
        }
    }

    /** Auto closeable contract method.
     * {@inheritDoc} */
    @Override
    public void close() {
        t.interrupt();
        // Avoid close possibly blocking.
        new Thread(new Runnable() {
            public void run() {
                try {
                  bsh.close();
                } catch (IOException e) { /* ignore */ }
            }
        }).start();
    }

    /** Called by Interpreter constructor for in reader.
     * @return new input stream reader instance.
     * {@inheritDoc} */
    @Override
    public Reader getIn() {
        try {
            return new InputStreamReader(new PipedInputStream(
                    OUT = new PipedOutputStream()));
        } catch (IOException e) {
            System.err.println("BshSession Exception in getIn: " + e);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public PrintStream getOut() {
        return System.out;
    }

    /** {@inheritDoc} */
    @Override
    public PrintStream getErr() {
        return System.err;
    }

    /** {@inheritDoc} */
    @Override
    public void println(Object o) {
        this.getOut().println(o);
    }

    /** {@inheritDoc} */
    @Override
    public void print(Object o) {
        this.getOut().print(o);
    }

    /** {@inheritDoc} */
    @Override
    public void error(Object o) {
        this.getErr().print(o);
    }

}
