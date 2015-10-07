package com.example.androidfunctionmitm;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;

public class MitmConfig {

    private final String CONFIG_FILE = "/data/data/com.example.androidfunctionmitm/config.yaml";

    private String packageName;
    private String serverIp;
    private List<HookDefinition> hookDefinitions;


    public Integer getServerPort() {
        return serverPort;
    }

    private Integer serverPort;

    public String getPackageName() {
        return packageName;
    }

    public String getServerIp() {
        return serverIp;
    }

    public MitmConfig() {
        YamlReader reader = null;
        hookDefinitions = new ArrayList<>();
        try {

            //XposedBridge.log("loading config file...");
            reader = new YamlReader(new FileReader(CONFIG_FILE));

            Object object = null;
            try {
                object = reader.read();
            } catch (YamlException e) {
                e.printStackTrace();
            }

            Map map = (Map) object;
            packageName = (String) map.get("packageName");
            serverIp = (String) map.get("serverIp");

            ArrayList<Map> hooks = (ArrayList) map.get("hooks");
            for (Map hook : hooks) {
                String classToHook = (String) hook.get("classToHook");
                String functionToHook = (String) hook.get("functionToHook");
                List<String> parameterTypes = null;


                boolean returnValueOnly = false;
                String returnType = null;
                if (hook.containsKey("returnValueOnly")) {
                    String returnOption = (String) hook.get("returnValueOnly");
                    if (returnOption.toLowerCase().equals("true")) {
                        returnValueOnly = true;
                    }
                    returnType = (String) hook.get("returnType");
                }

                try {
                    parameterTypes = (ArrayList) hook.get("parameterTypes");
                } catch (ClassCastException e) {
                    String temp = (String) hook.get("parameterTypes");
                    if (!(temp.equals("") && returnValueOnly)) {
                        XposedBridge.log("You must set the parameter types in the config.");
                        throw e;
                    }
                }

                HookDefinition hookDefinition = new HookDefinition(classToHook, functionToHook, parameterTypes, returnValueOnly, returnType);
                hookDefinitions.add(hookDefinition);
            }

            if (map.containsKey("serverPort")) {
                serverPort = (int) map.get("serverPort");
            } else {
                serverPort = 8080;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public List<HookDefinition> getHookDefinitions() {
        return hookDefinitions;
    }
}
