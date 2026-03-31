package com.google.android.exoplayer2.util;

import android.util.Pair;
import java.lang.Exception;

public interface ErrorMessageProvider<T extends Exception> {
    Pair<Integer, String> getErrorMessage(T t);
}
