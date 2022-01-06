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
package com.oracle.graal.python.pegparser;

import com.oracle.graal.python.pegparser.AbstractParser.NameDefaultPair;
import com.oracle.graal.python.pegparser.AbstractParser.SlashWithDefault;
import com.oracle.graal.python.pegparser.AbstractParser.StarEtc;
import com.oracle.graal.python.pegparser.sst.*;

public interface NodeFactory {
    public StmtTy createAnnAssignment(ExprTy target, ExprTy annotation, ExprTy rhs, boolean isSimple, int startOffset, int endOffset);

    public StmtTy createAssignment(ExprTy[] lhs, ExprTy rhs, String typeComment, int startOffset, int endOffset);

    public StmtTy createAugAssignment(ExprTy lhs, ExprTy.BinOp.Operator operation, ExprTy rhs, int startOffset, int endOffset);

    public ExprTy createBinaryOp(ExprTy.BinOp.Operator op, ExprTy left, ExprTy right, int startOffset, int endOffset);

    public ExprTy createComparison(ExprTy left, AbstractParser.CmpopExprPair[] rights, int startOffset, int endOffset);

    public ModTy createModule(StmtTy[] statements, int startOffset, int endOffset);

    public ExprTy createBooleanLiteral(boolean value, int startOffset, int endOffset);

    public ExprTy createNone(int startOffset, int endOffset);

    public ExprTy createEllipsis(int startOffset, int endOffset);

    default ExprTy createGetAttribute(ExprTy receiver, String name, int startOffset, int endOffset) {
        return createGetAttribute(receiver, name, null, startOffset, endOffset);
    }

    public ExprTy createGetAttribute(ExprTy receiver, String name, ExprContext context, int startOffset, int endOffset);

    public StmtTy createPass(int startOffset, int endOffset);

    public StmtTy createBreak(int startOffset, int endOffset);

    public StmtTy createExpression(ExprTy expr);

    public ExprTy createCall(ExprTy func, ExprTy[] args, KeywordTy[] kwargs, int startOffset, int endOffset);

    public StmtTy createContinue(int startOffset, int endOffset);

    public ExprTy createYield(ExprTy value, boolean isFrom, int startOffset, int endOffset);

    public ExprTy createNumber(String number, int startOffset, int endOffset);

    public StmtTy createWhile(ExprTy condition, StmtTy[] block, StmtTy[] elseBlock, int startOffset, int endOffset);

    public StmtTy createFor(ExprTy target, ExprTy iter, StmtTy[] block, StmtTy[] elseBlock, String typeComment, int startOffset, int endOffset);

    public ExprTy createString(String[] values, int startOffset, int endOffset, FExprParser exprParser, ParserErrorCallback errorCb);

    default ExprTy createSubscript(ExprTy receiver, ExprTy slice, int startOffset, int endOffset) {
        return createSubscript(receiver, slice, null, startOffset, endOffset);
    }

    public ExprTy createSubscript(ExprTy receiver, ExprTy slice, ExprContext context, int startOffset, int endOffset);

    public ExprTy createUnaryOp(ExprTy.UnaryOp.Operator op, ExprTy value, int startOffset, int endOffset);

    default ExprTy.Name createVariable(String name, int startOffset, int endOffset) {
        return createVariable(name, startOffset, endOffset, ExprContext.Load);
    }

    public ExprTy.Name createVariable(String name, int startOffset, int endOffset, ExprContext context);

    default ExprTy createTuple(ExprTy[] values, int startOffset, int endOffset) {
        return createTuple(values, null, startOffset, endOffset);
    }

    public ExprTy createTuple(ExprTy[] values, ExprContext context, int startOffset, int endOffset);

    default ExprTy createList(ExprTy[] values, int startOffset, int endOffset) {
        return createList(values, null, startOffset, endOffset);
    }

    public ExprTy createList(ExprTy[] values, ExprContext context, int startOffset, int endOffset);

    public ExprTy createDict(ExprTy[] keys, ExprTy[] values, int startOffset, int endOffset);

    public ExprTy createSet(ExprTy[] values, int startOffset, int endOffset);

    default ExprTy createStarred(ExprTy value, int startOffset, int endOffset) {
        return createStarred(value, null, startOffset, endOffset);
    }

    public ExprTy createStarred(ExprTy value, ExprContext context, int startOffset, int endOffset);

    public KeywordTy createKeyword(String arg, ExprTy value, int startOffset, int endOffset);

    public ArgTy createArgument(String argument, ExprTy annotation, String typeComment, int startOffset, int endOffset);

    public ArgumentsTy createArguments(ArgTy[] slashWithoutDefault, SlashWithDefault slashWithDefault, ArgTy[] paramWithoutDefault, NameDefaultPair[] paramWithDefault, StarEtc starEtc);

    public ComprehensionTy createComprehension(ExprTy target, ExprTy iter, ExprTy[] ifs, boolean isAsync, int startOffset, int endOffset);

    public ExprTy createListComprehension(ExprTy name, ComprehensionTy[] generators, int startOffset, int endOffset);

    public ExprTy createDictComprehension(AbstractParser.KeyValuePair name, ComprehensionTy[] generators, int startOffset, int endOffset);

    public ExprTy createSetComprehension(ExprTy name, ComprehensionTy[] generators, int startOffset, int endOffset);

    public ExprTy createGenerator(ExprTy name, ComprehensionTy[] generators, int startOffset, int endOffset);

    public StmtTy createFunctionDef(String name, ArgumentsTy args, StmtTy[] body, ExprTy[] decorators, ExprTy returns, String typeComment, int startOffset, int endOffset);
}
