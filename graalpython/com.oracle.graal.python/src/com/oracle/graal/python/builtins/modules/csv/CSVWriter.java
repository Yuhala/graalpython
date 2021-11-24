package com.oracle.graal.python.builtins.modules.csv;

import static com.oracle.graal.python.builtins.modules.csv.CSVModuleBuiltins.NOT_SET;
import static com.oracle.graal.python.builtins.modules.csv.QuoteStyle.QUOTE_NONE;

import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.object.PythonBuiltinObject;
import com.oracle.graal.python.lib.PyNumberCheckNode;
import com.oracle.graal.python.lib.PyObjectStrAsJavaStringNode;
import com.oracle.graal.python.nodes.ErrorMessages;
import com.oracle.graal.python.nodes.PRaiseNode;
import com.oracle.graal.python.nodes.control.GetNextNode;
import com.oracle.graal.python.nodes.object.IsBuiltinClassProfile;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;

public final class CSVWriter extends PythonBuiltinObject {

    Object write;        /* write output lines to this file */
    CSVDialect dialect;  /* parsing dialect */
    StringBuilder rec;   /* buffer for parser.join */
    int recSize;         /* size of allocated record */
    int numFields;       /* number of fields in record */

    public CSVWriter(Object cls, Shape instanceShape) {
        super(cls, instanceShape);
    }

    void joinReset() {
        this.rec = new StringBuilder();
        this.numFields = 0;
    }

    @TruffleBoundary
    void joinFields(Object iter) {
        Object field;

        this.joinReset();

        while (true) {
            try {
                field = GetNextNode.getUncached().execute(iter);
                this.joinField(field);
            } catch (PException e) {
                e.expectStopIteration(IsBuiltinClassProfile.getUncached());
                break;
            }
        }

        if (this.numFields > 0 && this.rec.length() == 0) {
            if (this.dialect.quoting == QUOTE_NONE) {
                throw PRaiseNode.getUncached().raise(PythonBuiltinClassType.CSVError, ErrorMessages.EMPTY_FIELD_RECORD_MUST_BE_QUOTED);
            }
            this.numFields--;
            this.joinAppend(null, true);
        }

        this.joinAppendLineterminator();
    }

    void joinField(Object field) {
        boolean quoted;

        switch (this.dialect.quoting) {
            case QUOTE_NONNUMERIC:
                quoted = !PyNumberCheckNode.getUncached().execute(field);
                break;
            case QUOTE_ALL:
                quoted = true;
                break;
            default:
                quoted = false;
                break;
        }

        if (field == PNone.NONE) {
            this.joinAppend(null, quoted);
        } else {
            String str = PyObjectStrAsJavaStringNode.getUncached().execute(field);
            this.joinAppend(str, quoted);
        }
    }

    boolean needsQuotes(Object field) {
        if (field == null)
            return false;

        boolean needsQuotes = false;
        String fieldStr = field.toString();
        final int strLen = fieldStr.length();

        for (int offset = 0; offset < strLen;) {
            boolean wantEscape = false;

            final int c = fieldStr.codePointAt(offset);
            if (c == dialect.delimiterCodePoint ||
                            c == dialect.escapeCharCodePoint ||
                            c == dialect.quoteCharCodePoint ||
                            dialect.lineTerminator.codePoints().anyMatch(cp -> cp == c)) {

                if (dialect.quoting == QUOTE_NONE ||
                                c == dialect.quoteCharCodePoint && !dialect.doubleQuote ||
                                c == dialect.escapeCharCodePoint) {
                    wantEscape = true;
                }

                if (!wantEscape) {
                    needsQuotes = true;
                    break;
                }
            }
            offset += Character.charCount(c);
        }

        return needsQuotes;
    }

    void joinAppend(Object field, boolean quoted) {
        CSVDialect dialect = this.dialect;

        /* If we don't already know that the field must be quoted due to
           dialect settings, check if the field contains characters due
           to which it must be quoted.
         */
        if (!quoted) {
            quoted = needsQuotes(field);
        }

        /* If this is not the first field we need a field separator */
        if (this.numFields > 0) {
            this.rec.append(dialect.delimiter);
        }

        /* Handle preceding quote */
        if (quoted) {
            this.rec.append(dialect.quoteChar);
        }

        /* Copy field data and add escape chars as needed */
        /* If field is null just pass over */
        if (field != null) {
            String fieldStr = field.toString();
            int strLen = fieldStr.length();

            /* Python supports utf-32 characters, as Java characters are utf-16 only,
             * we have to work with code points instead. */
            for (int offset = 0; offset < strLen; ) {

                boolean wantEscape = false;

                final int c = fieldStr.codePointAt(offset);

                if (c == dialect.delimiterCodePoint ||
                        c == dialect.escapeCharCodePoint ||
                        c == dialect.quoteCharCodePoint ||
                                dialect.lineTerminator.codePoints().anyMatch(cp -> cp == c)) {

                    if (dialect.quoting == QUOTE_NONE) {
                        wantEscape = true;
                    } else {
                        if (c == dialect.quoteCharCodePoint) {
                            if (dialect.doubleQuote) {
                                this.rec.append(dialect.quoteChar);
                            } else {
                                wantEscape = true;
                            }
                        } else if (c == dialect.escapeCharCodePoint) {
                            wantEscape = true;
                        }
                        if (!wantEscape) {
                            quoted = true;
                        }
                    }
                    if (wantEscape) {
                        if (dialect.escapeChar == NOT_SET) {
                            throw PRaiseNode.getUncached().raise(PythonBuiltinClassType.CSVError, ErrorMessages.ESCAPE_WITHOUT_ESCAPECHAR);
                        }
                        this.rec.append(dialect.escapeChar);
                    }
                }
                this.rec.appendCodePoint(c);
                offset += Character.charCount(c);
            }
        }
        if (quoted) {
            this.rec.append(dialect.quoteChar);
        }
        this.numFields++;
    }

    void joinAppendLineterminator() {
        this.rec.append(this.dialect.lineTerminator);
    }

}
