/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.nodes.attributes;

import com.oracle.graal.python.builtins.objects.type.SpecialMethodSlot;
import com.oracle.graal.python.nodes.PNodeWithContext;
import com.oracle.graal.python.nodes.call.special.LookupAndCallTernaryNode;
import com.oracle.graal.python.nodes.expression.ExpressionNode;
import com.oracle.graal.python.nodes.frame.WriteNode;
import com.oracle.graal.python.nodes.statement.StatementNode;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;

@NodeChild(value = "object", type = ExpressionNode.class)
@NodeChild(value = "rhs", type = ExpressionNode.class)
public abstract class SetAttributeNode extends StatementNode implements WriteNode {

    public static final class Dynamic extends PNodeWithContext {
        @Child private LookupAndCallTernaryNode call = LookupAndCallTernaryNode.create(SpecialMethodSlot.SetAttr);

        public void execute(VirtualFrame frame, Object object, Object key, Object value) {
            call.execute(frame, object, key, value);
        }

        public static Dynamic create() {
            return new Dynamic();
        }
    }

    private final TruffleString key;

    protected SetAttributeNode(TruffleString key) {
        this.key = key;
    }

    protected abstract ExpressionNode getObject();

    public abstract ExpressionNode getRhs();

    public static SetAttributeNode create(TruffleString key) {
        return create(key, null, null);
    }

    public static SetAttributeNode create(TruffleString key, ExpressionNode object, ExpressionNode rhs) {
        return SetAttributeNodeGen.create(key, object, rhs);
    }

    @Override
    public final void executeObject(VirtualFrame frame, Object value) {
        executeVoid(frame, getObject().execute(frame), value);
    }

    public abstract void executeVoid(VirtualFrame frame, Object object, Object value);

    public TruffleString getAttributeId() {
        return key;
    }

    public ExpressionNode getPrimaryNode() {
        return getObject();
    }

    @Specialization
    protected void doIt(VirtualFrame frame, Object object, Object value,
                    @Cached("create(SetAttr)") LookupAndCallTernaryNode call) {
        call.execute(frame, object, key, value);
    }
}
