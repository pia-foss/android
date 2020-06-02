/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.wireguard.util;

import android.os.Handler;

import com.privateinternetaccess.android.wireguard.backend.GoBackend.GhettoCompletableFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Helper class for running asynchronous tasks and ensuring they are completed on the main thread.
 */

public class AsyncWorker {
    private final Executor executor;
    private final Handler handler;

    public AsyncWorker(final Executor executor, final Handler handler) {
        this.executor = executor;
        this.handler = handler;
    }

    public GhettoCompletableFuture<Void> runAsync(final AsyncRunnable<?> runnable) {
        final GhettoCompletableFuture<Void> future = new GhettoCompletableFuture<>();
        executor.execute(() -> {
            try {
                runnable.run();
                handler.post(() -> future.complete(null));
            } catch (final Throwable t) {
                //handler.post(() -> future.completeExceptionally(t));
            }
        });
        return future;
    }

    @FunctionalInterface
    public interface AsyncRunnable<E extends Throwable> {
        void run() throws E;
    }

    @FunctionalInterface
    public interface AsyncSupplier<T, E extends Throwable> {
        T get() throws E;
    }
}
