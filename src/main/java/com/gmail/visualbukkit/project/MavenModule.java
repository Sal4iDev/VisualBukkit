package com.gmail.visualbukkit.project;

import org.json.JSONObject;

public abstract class MavenModule extends PluginModule {

    public MavenModule(String uid, String name) {
        super(uid, name);
    }

    public abstract JSONObject serialize();
}
