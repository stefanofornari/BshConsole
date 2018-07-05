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

import bsh.ClassIdentifier;
import bsh.ClassPathException;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.UtilEvalError;
import bsh.classpath.ClassManagerImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.AttributedString;

/**
 *
 */
public class BshCompleter implements Completer {

    private final Interpreter bsh;
    private final JlineNameCompletionTable names = new JlineNameCompletionTable();
    private final Map<String, String[]> memberCache = new WeakHashMap<>();

    /**
     *
     * @param bsh the beanshell interpreter - NOT NULL
     */
    public BshCompleter(Interpreter bsh) {
        if (bsh == null) {
            throw new IllegalArgumentException("bsh can not be null");
        }
        this.bsh = bsh;
        names.add(bsh.getNameSpace());
        if (bsh.getNameSpace().getClassManager() instanceof ClassManagerImpl) {
            try {
                names.add(((ClassManagerImpl) bsh.getNameSpace().getClassManager()).getClassPath());
            } catch (ClassPathException e) { /* ignore */ }
        }
    }

    private String[] findMembers(String name) {
        if (memberCache.containsKey(name)) {
            return memberCache.get(name);
        }
        try {
            Object obj = Primitive.unwrap(bsh.getNameSpace().get(name, bsh));
            if (null != obj) {
                Class<?> cls = (obj instanceof Class) ? (Class<?>) obj
                        : (obj instanceof ClassIdentifier)
                                ? ((ClassIdentifier) obj).getTargetClass() : obj.getClass();
                memberCache.put(name,
                        Stream.concat(Stream.of(cls.getFields()).map(Field::getName),
                                Stream.of(cls.getMethods()).map(Method::getName))
                                .distinct().map((name + ".")::concat).toArray(String[]::new));
                return memberCache.get(name);
            }
        } catch (UtilEvalError e) {/* ignore */ }
        
        return new String[0];
    }

    @Override
    public void complete(LineReader reader, ParsedLine pl, List<Candidate> candidates) {
        ArrayList<String> nameList = new ArrayList<>();
        names.getMatchingNames(pl.line(), nameList);
        
        for (String n : nameList) {
            candidates.add(new Candidate(AttributedString.stripAnsi(n), n, null, null, null, null, false));
        }
        for (String n : findMembers(pl.line().replaceFirst("\\.[^\\.]*$", ""))) {
            candidates.add(new Candidate(AttributedString.stripAnsi(n), n, null, null, null, null, false));
        }
    }

}
