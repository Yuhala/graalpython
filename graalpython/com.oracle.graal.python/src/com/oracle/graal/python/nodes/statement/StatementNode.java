/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 * Copyright (c) 2013, Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.graal.python.nodes.statement;

import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.nodes.PNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

/**
 * Base class for all statements. Statements normally never return a value, unless executed using
 * {@link #returnExecute(VirtualFrame)}.
 */
@GenerateWrapper
public abstract class StatementNode extends PNode {
    public static final StatementNode[] EMPTY_STATEMENT_ARRAY = new StatementNode[0];

    @CompilationFinal private boolean isTryBlock = false;
    @CompilationFinal private boolean returnFallThrough;

    public abstract void executeVoid(VirtualFrame frame);

    /**
     * Execution can directly return the value from return statement if it is the last statement
     * that was executed. This method may still throw
     * {@link com.oracle.graal.python.runtime.exception.ReturnException}.
     */
    public Object returnExecute(VirtualFrame frame) {
        executeVoid(frame);
        if (!returnFallThrough) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            returnFallThrough = true;
        }
        return PNone.NONE;
    }

    // Helper method
    protected final Object genericExecute(VirtualFrame frame, boolean isReturnExecute) {
        CompilerAsserts.partialEvaluationConstant(isReturnExecute);
        if (isReturnExecute) {
            return returnExecute(frame);
        } else {
            executeVoid(frame);
            return null;
        }
    }

    public void markAsTryBlock() {
        isTryBlock = true;
    }

    public boolean isTryBlock() {
        return isTryBlock;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new StatementNodeWrapper(this, probe);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return (tag == StandardTags.StatementTag.class) || (isTryBlock && tag == StandardTags.TryBlockTag.class) || super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        if (isTryBlock) {
            if (this.getParent() instanceof TryExceptNode) {
                return this.getParent();
            } else if (this.getParent() instanceof StatementNodeWrapper) {
                assert this.getParent().getParent() instanceof TryExceptNode;
                return this.getParent().getParent();
            }
        }
        return null;
    }
}
