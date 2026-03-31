package com.ua.mytrinity;

import android.app.Application;
import com.ua.mytrinity.player.TrinityPlayerBuilder;
import com.ua.mytrinity.player.TrinityPlayerBuilderImpl;

public class App extends Application {
    public TrinityPlayerBuilder getPlayerBuilder() {
        return new TrinityPlayerBuilderImpl();
    }
}
