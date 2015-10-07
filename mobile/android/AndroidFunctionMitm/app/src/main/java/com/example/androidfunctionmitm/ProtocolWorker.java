package com.example.androidfunctionmitm;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class ProtocolWorker {
    private HookDefinition hookDefinition;

    public ProtocolWorker(HookDefinition hookDefinition) {
        this.hookDefinition = hookDefinition;
    }

    public Object[] run(Object[] args) {
        SocketHandler socketHandler = SocketHandler.getInstance();
        XposedBridge.log("got socket handler");
        ArrayList<String> stringArgs = (ArrayList<String>) getStringArgs(args);
        XposedBridge.log("getting ready to send receive");
        List<String> newArgs = socketHandler.sendReceiveUntilSuccess(hookDefinition.getFunctionToHook(), stringArgs);
        XposedBridge.log("send receive done.");
        Object[] finalArgs = getObjectsFromStringArgs(newArgs, args);
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (finalArgs[i] == null) {
                    finalArgs[i] = args[i];
                }
                args[i] = finalArgs[i];
            }
        }
        return finalArgs;
    }

    protected Object[] getObjectsFromStringArgs(List<String> modifiedArgs, Object[] originalArgs) {
        ArrayList<Class<?>> argClasses = (ArrayList<Class<?>>) hookDefinition.getParameterTypeClasses();
        ArrayList<Object> objectArrayList = new ArrayList<>();

        for (int i = 0; i < modifiedArgs.size(); i++) {
            if (isBuiltInType(argClasses.get(i))) {
                Object newArg = convertArgToObject(argClasses.get(i), modifiedArgs.get(i));
                objectArrayList.add(newArg);
            } else {
                objectArrayList.add(originalArgs[i]);
            }
        }
        return objectArrayList.toArray();
    }

    /*
    Deserialization when getting objects back from socket.
     */
    protected Object convertArgToObject(Class<?> argClass, String arg) {

        switch (argClass.getName()) {
            case ("java.lang.String"): {
                return arg;
            }
            case ("byte"):
                return new Byte(arg).byteValue();
            case ("short"):
                return new Short(arg).shortValue();
            case ("int"):
                return new Integer(arg).intValue();
            case ("long"):
                return new Long(arg).longValue();
            case ("double"):
                return new Double(arg).doubleValue();
            case ("boolean"):
                return new Boolean(arg).booleanValue();
            case ("char"):
                return new Character(arg.charAt(0)).charValue();
            default:
//                XposedBridge.log("In default/ xstream handler");
//                XposedBridge.log("Class: " + argClass.getName());
//                XStream xstream = new XStream(new JettisonMappedXmlDriver());
//                xstream.alias(argClass.getName(), argClass);
//                XStream xstream = new XStream(new Sun14ReflectionProvider(
//                        new FieldDictionary(new ImmutableFieldKeySorter())),
//                        new DomDriver("utf-8"));
//                Object object = xstream.fromXML(arg);
//                return object;
                return "non builtin type arg.";
        }
    }

    protected List<String> getStringArgs(Object[] args) {
        ArrayList<Class<?>> argClasses = (ArrayList<Class<?>>) hookDefinition.getParameterTypeClasses();
        ArrayList<String> stringArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String argAsString = convertArgToString(argClasses.get(i), args[i]);
            stringArgs.add(argAsString);
        }

        return stringArgs;
    }

    /*
    Serialization before sending over the socket .
     */
    protected String convertArgToString(Class<?> argClass, Object arg) {
        if (isBuiltInType(argClass)) {
            try {
                return arg.toString();
            } catch (NullPointerException e) {
                return null;
            }
//            return "\"" + arg.toString() + "\"";
        } else {
            return "non builtin type arg.";
//            byte[] data = SerializationUtils.serialize(arg)
//            XStream xstream = new XStream(new JettisonMappedXmlDriver());
//            xstream.setMode(XStream.NO_REFERENCES);
//            xstream.alias(argClass.getName(), argClass);
//            XposedBridge.log("Tyring to convert object to json...");
//            String jsonRepresentation = xstream.toXML(arg);
//            XposedBridge.log(jsonRepresentation);
//            return jsonRepresentation;
        }
    }

    protected boolean isBuiltInType(Class<?> argClass) {
//        XposedBridge.log("class name is: " + argClass.getName());
        switch (argClass.getName()) {
            case ("java.lang.String"):
            case ("byte"):
            case ("short"):
            case ("int"):
            case ("long"):
            case ("double"):
            case ("boolean"):
            case ("char"):
                return true;
            default:
                return false;
        }
    }
}
