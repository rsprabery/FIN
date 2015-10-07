package com.example.androidfunctionmitm;


import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;

public class SendFunctionHook extends XC_MethodHook {

    //    private final SocketHandler socketHandler; = SocketHandler.getInstance();
    private HookDefinition hookDefinition;
    private ProtocolWorker protocolWorker;

    public SendFunctionHook(HookDefinition hookDefinition) {
        super();
        this.hookDefinition = hookDefinition;
        protocolWorker = new ProtocolWorker(hookDefinition);
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        if (!hookDefinition.isReturnValueOnly()) {
            if (isUIThread()) {
                AsyncProtocolHandler asyncProtocolHandler = new AsyncProtocolHandler();
                asyncProtocolHandler.execute(protocolWorker, param.args);
                param.args = asyncProtocolHandler.get();
            } else {
                protocolWorker.run(param.args);
            }
        }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        if (hookDefinition.isReturnValueOnly()) {
            if (isUIThread()) {
                AsyncProtocolHandler asyncProtocolHandler = new AsyncProtocolHandler();
                asyncProtocolHandler.execute(protocolWorker, new Object[]{param.getResult()});
                Object[] newValueArray = asyncProtocolHandler.get();
                param.setResult(newValueArray[0]);
            } else {
                Object[] newValueArray = protocolWorker.run(new Object[]{param.getResult()});
                param.setResult(newValueArray[0]);
            }
        }
    }

    public static boolean isUIThread() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            return true;
        } else {
            return false;
        }
    }

}
