/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.builtins.objects.thread;

import static com.oracle.graal.python.nodes.SpecialMethodNames.__ENTER__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.__EXIT__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.__REPR__;

import java.util.List;

import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.thread.LockBuiltinsFactory.AcquireLockNodeFactory;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonTernaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

@CoreFunctions(extendClasses = PythonBuiltinClassType.PLock)
public class LockBuiltins extends PythonBuiltins {
    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return LockBuiltinsFactory.getFactories();
    }

    @Builtin(name = "acquire", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 3, keywordArguments = {"waitflag", "timeout"})
    @GenerateNodeFactory
    abstract static class AcquireLockNode extends PythonTernaryBuiltinNode {
        @Specialization
        @TruffleBoundary
        boolean doAcquire(PLock self, int waitFlag, double timeout) {
            return self.acquire(waitFlag, timeout);
        }

        @Specialization
        @TruffleBoundary
        boolean doAcquire(PLock self, @SuppressWarnings("unused") PNone waitFlag, double timeout) {
            return self.acquire(1, timeout);
        }

        @Specialization
        @TruffleBoundary
        boolean doAcquire(PLock self, int waitFlag, @SuppressWarnings("unused") PNone timeout) {
            return self.acquire(waitFlag, -1.0);
        }

        @Specialization
        @TruffleBoundary
        boolean doAcquire(PLock self, @SuppressWarnings("unused") PNone waitFlag, @SuppressWarnings("unused") PNone timeout) {
            return self.acquire(1, -1.0);
        }

        public static AcquireLockNode create() {
            return AcquireLockNodeFactory.create();
        }
    }

    @Builtin(name = "acquire_lock", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 3, keywordArguments = {"waitflag", "timeout"})
    @GenerateNodeFactory
    abstract static class AcquireLockLockNode extends PythonTernaryBuiltinNode {
        @Specialization
        Object acquire(PLock self, Object waitFlag, Object timeout,
                        @Cached("create()") AcquireLockNode acquireLockNode) {
            return acquireLockNode.execute(self, waitFlag, timeout);
        }
    }

    @Builtin(name = __ENTER__, minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 3, keywordArguments = {"waitflag", "timeout"})
    @GenerateNodeFactory
    abstract static class EnterLockNode extends PythonTernaryBuiltinNode {
        @Specialization
        Object acquire(PLock self, Object waitFlag, Object timeout,
                        @Cached("create()") AcquireLockNode acquireLockNode) {
            return acquireLockNode.execute(self, waitFlag, timeout);
        }
    }

    @Builtin(name = "release", fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class ReleaseLockNode extends PythonUnaryBuiltinNode {
        @Specialization
        @TruffleBoundary
        Object doRelease(PLock self) {
            self.release();
            return PNone.NONE;
        }
    }

    @Builtin(name = __EXIT__, fixedNumOfPositionalArgs = 4)
    @GenerateNodeFactory
    abstract static class ExitLockNode extends PythonBuiltinNode {
        @Specialization
        @TruffleBoundary
        Object exit(PLock self, @SuppressWarnings("unused") Object type, @SuppressWarnings("unused") Object value, @SuppressWarnings("unused") Object traceback) {
            self.release();
            return PNone.NONE;
        }
    }

    @Builtin(name = "locked", fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class IsLockedLockNode extends PythonUnaryBuiltinNode {
        @Specialization
        boolean isLocked(PLock self) {
            return self.locked();
        }
    }

    @Builtin(name = __REPR__, fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class ReprLockNode extends PythonUnaryBuiltinNode {
        @Specialization
        @TruffleBoundary
        String repr(PLock self) {
            return String.format("<%s %s object at %s>",
                            (self.locked()) ? "locked" : "unlocked",
                            self.getPythonClass(),
                            self.hashCode());
        }
    }
}
