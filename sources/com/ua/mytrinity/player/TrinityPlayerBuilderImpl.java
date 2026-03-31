package com.ua.mytrinity.player;

public class TrinityPlayerBuilderImpl extends TrinityPlayerBuilder {
    public TrinityPlayer build() {
        return new TrinityPlayerExo(this.context, this.attrs, this.defStyle);
    }
}
