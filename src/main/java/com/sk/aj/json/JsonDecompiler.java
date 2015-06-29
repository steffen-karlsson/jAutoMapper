package com.sk.aj.json;

import com.google.gson.annotations.SerializedName;
import com.squareup.javapoet.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by steffenkarlsson on 6/28/15.
 */
final class JsonDecompiler {

    protected interface OnDecompileFinishedHandler {
        void onError(String error);
        void onFinished(JavaFile file);
    }

    private final Set<Class> WRAPPER_TYPES = new HashSet<Class>(Arrays.asList(
            Boolean.class, String.class, Integer.class, Long.class, Float.class, Double.class));

    private JavaFile handleJavaFile(TypeSpec classTypeSpec) {
        return JavaFile.builder(AutoJSON.mPackageName, classTypeSpec).build();
    }

    private TypeSpec handleClass(String name, ArrayList<FieldSpec> fields, ArrayList<MethodSpec> methods) {
        TypeSpec.Builder clzz = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC);
        for (FieldSpec field : fields)
            clzz.addField(field);

        if (methods != null && !methods.isEmpty()) {
            for (MethodSpec method : methods)
                clzz.addMethod(method);
        }
        return clzz.build();
    }

    private FieldSpec handleAttribute(Type type, String name) {
        return finalizeAttr(FieldSpec.builder(type, AutoJSON.mOnFormatCallback.formatAttr(name)), name);
    }

    private FieldSpec handleAttribute(String name) {
        return finalizeAttr(FieldSpec.builder(ClassName.get(AutoJSON.mPackageName,
                        AutoJSON.mOnFormatCallback.formatClass(name)),
                AutoJSON.mOnFormatCallback.formatAttr(name)), name);
    }

    private FieldSpec handleArray(Type type, String name) {
        return finalizeAttr(FieldSpec.builder(ParameterizedTypeName.get(List.class, type),
                AutoJSON.mOnFormatCallback.formatAttr(name)), name);
    }

    private FieldSpec handleArray(TypeName typeName, String name) {
        return finalizeAttr(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), typeName),
                AutoJSON.mOnFormatCallback.formatAttr(name)), name);
    }

    private FieldSpec finalizeAttr(FieldSpec.Builder builder, String annotatioName) {
        builder.addModifiers(Modifier.PROTECTED);
        builder.addAnnotation(getAnnotation(annotatioName));
        return builder.build();
    }

    private AnnotationSpec getAnnotation(String jsonName) {
        return AnnotationSpec.builder(SerializedName.class)
                .addMember("value", "$S", jsonName)
                .build();
    }

    public static void handleRootAsArray(JSONArray jsonArray, OnDecompileFinishedHandler result) {
        //TODO: Handle array as root
    }

    private FieldSpec loopHandleRootAsObject(JSONObject jsonObject, String parentKey, OnDecompileFinishedHandler result) {
        ArrayList<FieldSpec> fields = new ArrayList<>();
        ArrayList<MethodSpec> methods = new ArrayList<>();

        for (Object k : jsonObject.keySet()) {
            if (k instanceof String) {
                String key = (String) k;
                // Check if keyword is ignored
                if (AutoJSON.mIgnores.containsKey(key))
                    continue;

                FieldSpec field = null;
                Object obj = jsonObject.get(key);
                if (isNestedJson(obj)) {
                    if (obj instanceof JSONObject) {
                        // Handling the case of a nested json object
                        field = loopHandleRootAsObject((JSONObject) obj, key, result);
                    } else if (obj instanceof JSONArray) {
                        // Handling the case of a nested json array
                        Object firstItem = ((JSONArray) obj).get(0);
                        String listName = String.format("%ss", key);
                        if (WRAPPER_TYPES.contains(firstItem.getClass()))
                            // Primitive array type
                            field = handleArray(firstItem.getClass(), listName);
                        else if (firstItem instanceof JSONObject) {
                            FieldSpec spec = loopHandleRootAsObject((JSONObject)firstItem, key, result);
                            field = handleArray(spec.type, listName);
                        } else
                            result.onError(AutoJSON.TAG + "Unsupported key type: " + firstItem.getClass().getSimpleName());
                    }
                } else
                    field = handleAttribute(obj.getClass(), key);

                if (field != null) {
                    fields.add(field);

                    // Adding getter and setter method if specified
                    if (AutoJSON.mGetters.containsKey(key))
                        methods.add(GetterSetter.generateGetter(key, field.type));
                    if (AutoJSON.mSetters.containsKey(key))
                        methods.add(GetterSetter.generateSetter(key, field.type));
                }
            } else
                result.onError(AutoJSON.TAG + "Unsupported key type: " + k.getClass().getSimpleName());
        }

        TypeSpec clzz = handleClass(AutoJSON.mOnFormatCallback.formatClass(parentKey), fields, methods);
        result.onFinished(handleJavaFile(clzz));
        return handleAttribute(parentKey);
    }

    public static void handleRootAsObject(JSONObject jsonObject, OnDecompileFinishedHandler result) {
        JsonDecompiler.getInstance().loopHandleRootAsObject(jsonObject, AutoJSON.mClassName, result);
    }

    private static boolean isNestedJson(Object possibleJson) {
        return possibleJson instanceof JSONObject || possibleJson instanceof JSONArray;
    }

    private static JsonDecompiler ourInstance = new JsonDecompiler();
    private static JsonDecompiler getInstance() {
        return ourInstance;
    }

    private JsonDecompiler() {
    }
}
