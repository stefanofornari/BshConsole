package ste.beanshell.commands;

import org.jline.reader.impl.LineReaderImpl;

import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;

public class getBshPrompt
{
    public static LineReaderImpl lineReader;
    public static final String prompt = "\u001b[34;1mbsh #\u001b[0m ";

    public static String usage() {
        return "usage: getBshPrompt( )";
    }

    public static Object invoke( Interpreter env, CallStack callstack ) {
        if (null == lineReader)
            initLineReader(env);

        lineReader.setPrompt(prompt);
        return "";
    }

    private static void initLineReader(Interpreter env) {
        try {
            lineReader = (LineReaderImpl) env.get("bsh.lineReader");
        } catch (EvalError e) {
            throw new InterpreterError("Error initializing getBshPrompt", e);
        }
    }
}
