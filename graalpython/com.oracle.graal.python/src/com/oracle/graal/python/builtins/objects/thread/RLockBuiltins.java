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
import com.oracle.graal.python.builtins.objects.thread.RLockBuiltinsFactory.AcquireRLockNodeFactory;
import com.oracle.graal.python.builtins.objects.tuple.PTuple;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonTernaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.runtime.exception.PythonErrorType;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;

@CoreFunctions(extendClasses = PythonBuiltinClassType.PRLock)
public class RLockBuiltins extends PythonBuiltins {
    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return RLockBuiltinsFactory.getFactories();
    }

    @Builtin(name = "acquire", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 3, keywordArguments = {"waitflag", "timeout"})
    @GenerateNodeFactory
    abstract static class AcquireRLockNode extends PythonTernaryBuiltinNode {
        @Specialization
        @TruffleBoundary
        boolean doAcquire(PRLock self, int waitFlag, double timeout) {
            return self.acquire(waitFlag, timeout);
        }

        @Specialization
        @TruffleBoundary
        boolean doAcquire(PRLock self, @SuppressWarnings("unused") PNone waitFlag, double timeout) {
            return self.acquire(1, timeout);
        }

        @Specialization
        @TruffleBoundary
        boolean doAcquire(PRLock self, int waitFlag, @SuppressWarnings("unused") PNone timeout) {
            return self.acquire(waitFlag, -1.0);
        }

        @Specialization
        @TruffleBoundary
        boolean doAcquire(PRLock self, @SuppressWarnings("unused") PNone waitFlag, @SuppressWarnings("unused") PNone timeout) {
            return self.acquire(1, -1.0);
        }

        public static AcquireRLockNode create() {
            return AcquireRLockNodeFactory.create();
        }
    }

    @Builtin(name = __ENTER__, minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 3, keywordArguments = {"waitflag", "timeout"})
    @GenerateNodeFactory
    abstract static class EnterRLockNode extends PythonTernaryBuiltinNode {
        @Specialization
        Object acquire(PRLock self, Object waitFlag, Object timeout,
                        @Cached("create()") AcquireRLockNode acquireLockNode) {
            return acquireLockNode.execute(self, waitFlag, timeout);
        }
    }

    @Builtin(name = "release", fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class ReleaseRLockNode extends PythonUnaryBuiltinNode {
        @Specialization
        @TruffleBoundary
        Object doRelease(PRLock self) {
            self.release();
            return PNone.NONE;
        }
    }

    @Builtin(name = __EXIT__, fixedNumOfPositionalArgs = 4)
    @GenerateNodeFactory
    abstract static class ExitRLockNode extends PythonBuiltinNode {
        @Specialization
        @TruffleBoundary
        Object exit(PRLock self, @SuppressWarnings("unused") Object type, @SuppressWarnings("unused") Object value, @SuppressWarnings("unused") Object traceback) {
            self.release();
            return PNone.NONE;
        }
    }

    @Builtin(name = "_is_owned", fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class IsOwnedRLockNode extends PythonUnaryBuiltinNode {
        @Specialization
        boolean isOwned(PRLock self) {
            return self.isOwned();
        }
    }

    @Builtin(name = "_acquire_restore", fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class AcquireRestoreRLockNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object acquireRestore(PRLock self) {
            if (!self.tryToAcquire()) {
                self.acquire();
            }
            return PNone.NONE;
        }
    }

    @Builtin(name = "_release_save", fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class ReleaseSaveRLockNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object releaseSave(PRLock self,
                        @Cached("createBinaryProfile()") ConditionProfile countProfile) {
            int count = self.getCount();
            if (countProfile.profile(count == 0)) {
                throw raise(PythonErrorType.RuntimeError, "cannot release un-acquired lock");
            }
            PTuple retVal = factory().createTuple(new Object[]{count, self.getOwnerId()});
            self.releaseAll();
            return retVal;
        }
    }

    @Builtin(name = __REPR__, fixedNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class ReprRLockNode extends PythonUnaryBuiltinNode {
        @Specialization
        @TruffleBoundary
        String repr(PRLock self) {
            return String.format("<%s %s object owner=%d count=%d at %s>",
                            (self.locked()) ? "locked" : "unlocked",
                            self.getPythonClass(),
                            self.getOwnerId(),
                            self.getCount(),
                            self.hashCode());
        }
    }
}
