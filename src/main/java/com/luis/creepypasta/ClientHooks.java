package com.luis.creepypasta;

import java.util.function.IntConsumer;

public final class ClientHooks {
    public static IntConsumer SCREAMER = ticks -> { };

    private ClientHooks() { }

    public static void triggerScreamer(int ticks) {
        SCREAMER.accept(ticks);
    }
}
