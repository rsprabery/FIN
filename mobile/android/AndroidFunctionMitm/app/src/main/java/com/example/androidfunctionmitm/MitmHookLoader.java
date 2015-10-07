package com.example.androidfunctionmitm;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class MitmHookLoader implements IXposedHookLoadPackage {

    public void handleLoadPackage(final LoadPackageParam lpparm) throws Throwable {

        MitmConfig mitmConfig = new MitmConfig();
        if (!lpparm.packageName.equals(mitmConfig.getPackageName()))
            return;

        XposedBridge.log("We're in " + lpparm.packageName);

        for (HookDefinition hookDefinition: mitmConfig.getHookDefinitions()) {
            Object[] paramterTypes = hookDefinition.getParameterTypes();
            Object[] hookMethodParams = new Object[paramterTypes.length + 1];
            for (int i = 0; i < paramterTypes.length; i++) {
                hookMethodParams[i] = paramterTypes[i];
            }

            hookMethodParams[hookMethodParams.length - 1] = new SendFunctionHook(hookDefinition);

            try {
                findAndHookMethod(hookDefinition.getClassToHook(), lpparm.classLoader,
                        hookDefinition.getFunctionToHook(), hookMethodParams);
            } catch (NoClassDefFoundError e) {
                XposedBridge.log("ERROR IN CONFIG:\n---Could not find class: " + hookDefinition.getClassToHook() + " ---");
            } catch (NoSuchMethodError e) {
                XposedBridge.log("ERROR IN CONFIG:\n---There is probably an issue with your arg types.");
                XposedBridge.log("The error was in function: " + hookDefinition.getFunctionToHook());
            }
        }
    }

}

