/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.builtins.objects.common;

import java.util.Iterator;

import com.oracle.graal.python.builtins.objects.function.PArguments.ThreadState;
import com.oracle.graal.python.nodes.PRaiseNode;
import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.nodes.Node;

/*
 * TODO:
 *
 * - EconomicMapStorage should implement all combinatory messages specialized to
 * another EconomicMapStorage. All of these should then be able to avoid any
 * calls to __hash__ and __eq__
 */

/**
 * A library to interact with our different storage strategies for dictionaries and sets.
 * Generalizations are encoded inside the implementations of the messages - each mutating message
 * potentially returns a new store.
 *
 * A few words of caution are in order.
 *
 * Python always calls {@code __hash__} on keys when they are set or retrieved. Thus, all strategies
 * implementing this library must ensure to call it on the {@code key} argument to the methods
 * {@link #hasKey}, {@link #getItem}, and {@link #setItem}. However, they <i>must not</i> call
 * {@code __hash__} repeatedly on keys that are already inserted, e.g. when comparing them to an
 * argument. Thus, storages must ensure to cache the hash value if a call could be observed.
 */
@GenerateLibrary
@SuppressWarnings("unused")
public abstract class HashingStorageLibrary extends Library {
    /**
     * @return the length of {@code self}
     */
    public int lengthWithState(HashingStorage self, ThreadState state) {
        if (state == null) {
            throw new AbstractMethodError("HashingStorageLibrary.lengthWithState");
        }
        return length(self);
    }

    /**
     * @see #lengthWithState(HashingStorage, ThreadState)
     */
    public int length(HashingStorage self) {
        return lengthWithState(self, null);
    }

    /**
     * Implementers <i>must</i> call {@code __hash__} on the key if that could be visible, to comply
     * with Python semantics.
     *
     * @return {@code true} if the object is contained in the store, {@code
     * false} otherwise.
     */
    public boolean hasKeyWithState(HashingStorage self, Object key, ThreadState state) {
        return getItemWithState(self, key, state) != null;
    }

    /**
     * @see #hasKeyWithState(HashingStorage, Object, ThreadState)
     */
    public boolean hasKey(HashingStorage self, Object key) {
        return getItem(self, key) != null;
    }

    /**
     * Implementers <i>must</i> call {@code __hash__} on the key if that could be visible, to comply
     * with Python semantics.
     *
     * @return the value associated with {@code key}, or {@code null}, if no such key is in the
     *         store.
     */
    public Object getItemWithState(HashingStorage self, Object key, ThreadState state) {
        if (state == null) {
            throw new AbstractMethodError("HashingStorageLibrary.getItemWithState");
        }
        return getItem(self, key);
    }

    /**
     * @see #getItemWithState(HashingStorage, Object, ThreadState)
     */
    public Object getItem(HashingStorage self, Object key) {
        return getItemWithState(self, key, null);
    }

    /**
     * Implementers <i>must</i> call {@code __hash__} on the key if that could be visible, to comply
     * with Python semantics.
     *
     * @param key : the key to store under
     * @param value : the value to store
     * @return the new store to use from now on, {@code self} has become invalid.
     */
    public HashingStorage setItemWithState(HashingStorage self, Object key, Object value, ThreadState state) {
        if (state == null) {
            throw new AbstractMethodError("HashingStorageLibrary.setItemWithState");
        }
        return setItem(self, key, value);
    }

    /**
     * @see #setItemWithState(HashingStorage, Object, Object, ThreadState)
     */
    public HashingStorage setItem(HashingStorage self, Object key, Object value) {
        return setItemWithState(self, key, value, null);
    }

    /**
     * Implementers <i>must</i> call {@code __hash__} on the key if that could be visible, to comply
     * with Python semantics.
     *
     * @param key : the key to store under
     * @return the new store to use from now on, {@code self} has become invalid.
     */
    public HashingStorage delItemWithState(HashingStorage self, Object key, ThreadState state) {
        if (state == null) {
            throw new AbstractMethodError("HashingStorageLibrary.delItem");
        }
        return delItem(self, key);
    }

    public HashingStorage delItem(HashingStorage self, Object key) {
        return delItemWithState(self, key, null);
    }

    /**
     * A node to be called in a loop with different keys to operate on {@code
     * store}. It's execute method returns the new store to use for the next iteration.
     */
    public abstract static class InjectIntoNode extends Node {
        public abstract HashingStorage[] execute(HashingStorage[] accumulator, Object key);
    }

    /**
     * Iterate over {@code self} keys and execute {@code node} with an accumulator and each key. The
     * first iteration uses {@code firstValue} as the first argument to
     * {@link InjectIntoNode#execute}. Each subsequent iteration uses the return value of the prior
     * iteration.
     *
     * @return the return value of the last call to {@link InjectIntoNode#execute}
     */
    public HashingStorage[] injectInto(HashingStorage self, HashingStorage[] firstValue, InjectIntoNode node) {
        throw new AbstractMethodError("HashingStorageLibrary.injectInto");
    }

    /**
     * Stores all key-value pairs from {@code self} into {@code other}, replacing key-value pairs
     * that already exist. The message is provided in this direction, so that the source can
     * optimize its iteration over its own entries. Note that this could of course be implemented
     * using injectInto, but we provide it as a separate message so that implementers can optimize
     * this operation.
     *
     * @return the new store to use for {@code other} from now on, the previous {@code other} has
     *         become invalid.
     */
    public HashingStorage addAllToOther(HashingStorage self, HashingStorage other) {
        throw new AbstractMethodError("HashingStorageLibrary.addAllToOther");
    }

    /**
     * @return an empty store to use from now on, {@code self} has become invalid.
     */
    public abstract HashingStorage clear(HashingStorage self);

    /**
     * @return a copy of this store, potentially with a different storage.
     */
    public HashingStorage copy(HashingStorage self) {
        throw new AbstractMethodError("HashingStorageLibrary.copy");
    }

    /**
     * @return {@code true} if the key-value pairs are equal between these storages.
     */
    public boolean equalsWithState(HashingStorage self, HashingStorage other, ThreadState state) {
        if (state == null) {
            throw new AbstractMethodError("HashingStorageLibrary.equalsWithState");
        }
        return equals(self, other);
    }

    /**
     * @see #equalsWithState(HashingStorage, HashingStorage, ThreadState)
     */
    public boolean equals(HashingStorage self, HashingStorage other) {
        return equalsWithState(self, other, null);
    }

    /**
     * @return {@code 0} if the set of keys is equal, {@code -1} if {@code self} is a subset, or
     *         {@code 1} if neither.
     */
    public int compareKeysWithState(HashingStorage self, HashingStorage other, ThreadState state) {
        if (state == null) {
            throw new AbstractMethodError("HashingStorageLibrary.compareKeysWithState");
        }
        return compareKeys(self, other);
    }

    /**
     * @see #compareKeysWithState(HashingStorage, HashingStorage, ThreadState)
     */
    public int compareKeys(HashingStorage self, HashingStorage other) {
        return compareKeysWithState(self, other, null);
    }

    /**
     * @return {@code 0} if all entries are equal, {@code -1} if {@code self} is a subset, or
     *         {@code 1} if neither.
     */
    public int compareEntriesWithState(HashingStorage self, HashingStorage other, ThreadState state) {
        throw new AbstractMethodError("HashingStorageLibrary.compareEntriesWithState");
    }

    /**
     * @return the intersection of the two storages, keeping the values from {@code other}.
     */
    public HashingStorage intersect(HashingStorage self, HashingStorage other) {
        throw new AbstractMethodError("HashingStorageLibrary.intersect");
    }

    /**
     * @return the xor of the two storages.
     */
    public HashingStorage xor(HashingStorage self, HashingStorage other) {
        throw new AbstractMethodError("HashingStorageLibrary.xor");
    }

    /**
     * @return the union of the two storages, by keys, keeping the values from {@code other} in case
     *         of conflicts.
     */
    public HashingStorage union(HashingStorage self, HashingStorage other) {
        throw new AbstractMethodError("HashingStorageLibrary.union");
    }

    /**
     * @return the a storage with all keys of {@code self} that are not also in {@code other}
     */
    public HashingStorage diff(HashingStorage self, HashingStorage other) {
        throw new AbstractMethodError("HashingStorageLibrary.diff");
    }

    /**
     * This method can be used to iterate over the keys of a store. Due to the nature of Java
     * iterators being an interface and the different storage strategies, this may be slow and
     * should be used with caution.
     *
     * @return an iterator over the keys in this store.
     */
    public Iterator<Object> keys(HashingStorage self) {
        throw new AbstractMethodError("HashingStorageLibrary.keys");
    }

    /**
     * This method can be used to iterate over the values of a store. Due to the nature of Java
     * iterators being an interface and the different storage strategies, this may be slow and
     * should be used with caution.
     *
     * @return an iterator over the values in this store.
     */
    public abstract Iterator<Object> values(HashingStorage self);

    /**
     * This method can be used to iterate over the key-value pairs of a store. Due to the nature of
     * Java iterators being an interface and the different storage strategies, this may be slow and
     * should be used with caution.
     *
     * @return an iterator over the keys-value pairs in this store.
     */
    public abstract Iterator<HashingStorage.DictEntry> entries(HashingStorage self);

    static final LibraryFactory<HashingStorageLibrary> FACTORY = LibraryFactory.resolve(HashingStorageLibrary.class);

    public static LibraryFactory<HashingStorageLibrary> getFactory() {
        return FACTORY;
    }

    public static HashingStorageLibrary getUncached() {
        return FACTORY.getUncached();
    }
}
