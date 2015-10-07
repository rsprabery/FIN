package com.example.androidfunctionmitm;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class HookDefinition {
    private String classToHook;
    private String functionToHook;
    private List<String> parameterTypes;
    private boolean returnValueOnly;
    private List<String> returnParameterTypes;

    public HookDefinition(String classToHook, String functionToHook, List<String> parameterTypes, boolean returnValueOnly, String returnParamterType) {
        this.classToHook = classToHook;
        this.functionToHook = functionToHook;
        this.parameterTypes = parameterTypes;
        this.returnValueOnly = returnValueOnly;
        this.returnParameterTypes = new ArrayList<>();
        this.returnParameterTypes.add(returnParamterType);
    }

    public String getClassToHook() {
        return classToHook;
    }

    public String getFunctionToHook() {
        return functionToHook;
    }

    public Object[] getParameterTypes() {
//        XposedBridge.log("Parameter type length: " + getParameterTypeClasses().size());
//        XposedBridge.log("Parameter types: " + getParameterTypeClasses().toString());
        if (isReturnValueOnly()) {
            return new Object[]{};
        } else {
            return getParameterTypeClasses().toArray();
        }
    }

    public List<String> getStringParameterTypes() {
        if (isReturnValueOnly()) {
            return returnParameterTypes;
        } else {
            return parameterTypes;
        }
    }

    public List<Class<?>> getParameterTypeClasses() {
        ArrayList<Class<?>> classArray = new ArrayList<>();
        for (String paramterType : getStringParameterTypes()) {
            try {
                String className = paramterType;
                Class classToAdd = null;
                if (className.equals("String")) {
                    className = "java.lang.String";
                    classToAdd = Class.forName(className);
                } else if (className.equals("byte")) {
                    classToAdd = Byte.TYPE;
                } else if (className.equals("short")) {
                    classToAdd = Short.TYPE;
                } else if (className.equals("int")) {
                    classToAdd = Integer.TYPE;
                } else if (className.equals("long")) {
                    classToAdd = Long.TYPE;
                } else if (className.equals("float")) {
                    classToAdd = Float.TYPE;
                } else if (className.equals("double")) {
                    classToAdd = Double.TYPE;
                } else if (className.equals("boolean")) {
                    classToAdd = Boolean.TYPE;
                } else if (className.equals("char")) {
                    classToAdd = Character.TYPE;
                } else if (className.equals("")) {
                    classToAdd = null;
                } else {
                    XposedBridge.log("Adding non standard class");
                    XposedBridge.log(className);
                    classToAdd = Class.forName(className);
                    XposedBridge.log(classToAdd.toString());
                }

                if (!(classToAdd == null)) {
                    classArray.add(classToAdd);
                }
//                XposedBridge.log("Adding parameter type: " + paramterType);
//                XposedBridge.log("Changed class parameter type to: " + className);
            } catch (ClassNotFoundException e) {
                XposedBridge.log("Can't find class, please re-edit your config file.");
                XposedBridge.log(e.getMessage());
                e.printStackTrace();
            }
        }

        return classArray;
    }

    public boolean isReturnValueOnly() {
        return returnValueOnly;
    }
}
