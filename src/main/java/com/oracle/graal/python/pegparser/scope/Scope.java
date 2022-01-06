/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.graal.python.pegparser.scope;

import com.oracle.graal.python.pegparser.sst.SSTNode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

/**
 * Roughly equivalent to CPython's {@code symtable_entry}.
 */
public class Scope {

    Scope(ScopeType type, SSTNode ast) {
        this.type = type;
        this.startOffset = ast.getStartOffset();
        this.endOffset = ast.getEndOffset();
    }

    enum ScopeType {
        Function,
        Class,
        Module,
        Annotation;
    }

    enum DefUse {
        DefGlobal,
        DefLocal,
        DefParam,
        DefNonLocal,
        Use,
        DefFree,
        DefFreeClass,
        DefImport,
        DefAnnot,
        DefCompIter,
        // shifted VariableScope flags
        Local,
        GlobalExplicit,
        GlobalImplicit,
        Free,
        Cell;

        static EnumSet<DefUse> DefBound = EnumSet.of(DefLocal, DefParam, DefImport);
    };

    HashMap<String, EnumSet<DefUse>> symbols;

    String name;
    ArrayList<String> varnames;
    ArrayList<Scope> children;
    ArrayList<String> directives;
    ScopeType type;

    enum ScopeFlags {
        IsNested,
        HasFreeVars,
        HasChildWithFreeVars,
        IsGenerator,
        IsCoroutine,
        IsComprehension,
        HasVarArgs,
        HasVarKeywords,
        ReturnsAValue,
        NeedsClassClosure,
        IsVisitingIterTarget;
    }

    EnumSet<ScopeFlags> flags;
    int comprehensionIterExpression;

    int startOffset;
    int endOffset;

    @Override
    public String toString() {
        return toString(0);
    }

    String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        sb.append("Scope ").append(name).append(" ").append(type).append("\n");
        if (!flags.isEmpty()) {
            for (int i = 0; i < indent; i++) {
                sb.append("    ");
            }
            sb.append("Flags: ").append(flags).append("\n");
        }
        if (!varnames.isEmpty()) {
            for (int i = 0; i < indent; i++) {
                sb.append("    ");
            }
            sb.append("Varnames: ").append(varnames.get(0));
            for (int i = 1; i < varnames.size(); i++) {
                sb.append(", ").append(varnames.get(i));
            }
        }
        for (Scope child : children) {
            sb.append(child.toString(indent + 1));
        }
        return sb.toString();
    }
}
