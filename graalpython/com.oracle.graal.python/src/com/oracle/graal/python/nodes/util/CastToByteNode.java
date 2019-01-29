/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.nodes.util;

import static com.oracle.graal.python.runtime.exception.PythonErrorType.TypeError;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.ValueError;

import java.util.function.Function;

import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.nodes.PNodeWithContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class CastToByteNode extends PNodeWithContext {
    public static final String INVALID_BYTE_VALUE = "byte must be in range(0, 256)";

    private final Function<Object, Byte> rangeErrorHandler;
    private final Function<Object, Byte> typeErrorHandler;

    protected CastToByteNode(Function<Object, Byte> rangeErrorHandler, Function<Object, Byte> typeErrorHandler) {
        this.rangeErrorHandler = rangeErrorHandler;
        this.typeErrorHandler = typeErrorHandler;
    }

    public abstract byte execute(Object val);

    @Specialization
    protected byte doByte(byte value) {
        return value;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected byte doInt(int value) {
        return PInt.byteValueExact(value);
    }

    @Specialization(replaces = "doInt")
    protected byte doIntOvf(int value) {
        try {
            return PInt.byteValueExact(value);
        } catch (ArithmeticException e) {
            return handleRangeError(value);
        }
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected byte doLong(long value) {
        return PInt.byteValueExact(value);
    }

    @Specialization(replaces = "doLong")
    protected byte doLongOvf(long value) {
        try {
            return PInt.byteValueExact(value);
        } catch (ArithmeticException e) {
            return handleRangeError(value);
        }
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected byte doPInt(PInt value) {
        return PInt.byteValueExact(value.longValueExact());
    }

    @Specialization(replaces = "doPInt")
    protected byte doPIntOvf(PInt value) {
        try {
            return PInt.byteValueExact(value.longValueExact());
        } catch (ArithmeticException e) {
            return handleRangeError(value);
        }
    }

    @Specialization
    protected byte doBoolean(boolean value) {
        return value ? (byte) 1 : (byte) 0;
    }

    @Fallback
    protected byte doGeneric(@SuppressWarnings("unused") Object val) {
        if (typeErrorHandler != null) {
            return typeErrorHandler.apply(val);
        } else {
            throw raise(TypeError, "an integer is required (got type %p)", val);
        }
    }

    private byte handleRangeError(Object val) {
        if (rangeErrorHandler != null) {
            return rangeErrorHandler.apply(val);
        } else {
            throw raise(ValueError, INVALID_BYTE_VALUE);
        }
    }

    public static CastToByteNode create() {
        return CastToByteNodeGen.create(null, null);
    }

    public static CastToByteNode create(Function<Object, Byte> rangeErrorHandler, Function<Object, Byte> typeErrorHandler) {
        return CastToByteNodeGen.create(rangeErrorHandler, typeErrorHandler);
    }

}