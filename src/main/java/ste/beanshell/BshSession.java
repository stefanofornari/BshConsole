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
    private Reader  IN;
    private PipedOutputStream OUT;
    public Interpreter bsh;
    private BshSession() {
        bsh = new Interpreter(this);
        bsh.setExitOnEOF(false);
    }

    public static BshSession init() {
        return instance;
    }

    public BshSession start() {
        (t = new Thread(bsh)).start();
        return instance;
    }

    public BshSession restart() {
        t.interrupt();
        try {
            IN.close();
            IN = null;
        } catch (IOException e) { /* ignore */ }
        bsh = new Interpreter(this, bsh.getNameSpace(), bsh);
        bsh.setExitOnEOF(false);
        return this.start();
    }

    public void close() {
        if (null == t)
            return;
        t.interrupt();
        t = null;
        try {
            bsh.setExitOnEOF(true);
            bsh.close();
        } catch (IOException e) { /* ignore */ }
    }

    public void write(byte b[]) {
        try {
            this.OUT.write(b);
            this.OUT.flush();
        } catch (IOException e) { e.printStackTrace();/* ignore */ }
    }

    @Override
    public Reader getIn() {
        if (null == IN) try {
            IN  = new InputStreamReader(new PipedInputStream(
                    OUT = new PipedOutputStream()));
        } catch (IOException e) { /* ignore */ }
        return IN;
    }

    @Override
    public PrintStream getOut() {
        return System.out;
    }

    @Override
    public PrintStream getErr() {
        return System.err;
    }

    @Override
    public void println(Object o) {
        this.getOut().println(o);
    }

    @Override
    public void print(Object o) {
        this.getOut().print(o);
    }

    @Override
    public void error(Object o) {
        this.getErr().println(o);
    }

}
