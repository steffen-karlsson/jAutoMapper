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

    private final OnDecompileFinishedHandler mResultListener;
    private final AutoJSON.Builder mSettings;

    protected interface OnDecompileFinishedHandler {
        void onError(String error);
        void onFinished(JavaFile file);
    }

    private final Set<Class> WRAPPER_TYPES = new HashSet<Class>(Arrays.asList(
            Boolean.class, String.class, Integer.class, Long.class, Float.class, Double.class));

    private JavaFile handleJavaFile(TypeSpec classTypeSpec) {
        return JavaFile.builder(mSettings.mPackageName, classTypeSpec).build();
    }

    private void handleClass(String name, List<FieldSpec> fields, List<MethodSpec> methods) {
        TypeSpec.Builder clzz = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC);
        for (FieldSpec field : fields)
            clzz.addField(field);

        if (methods != null && !methods.isEmpty()) {
            for (MethodSpec method : methods)
                clzz.addMethod(method);
        }
        TypeSpec spec = clzz.build();
        mResultListener.onFinished(handleJavaFile(spec));
    }

    private FieldSpec handleAttribute(Type type, String name) {
        return finalizeAttr(FieldSpec.builder(type, mSettings.mOnFormatCallback.formatAttr(name)), name);
    }

    private FieldSpec handleAttribute(String name) {
        return finalizeAttr(FieldSpec.builder(ClassName.get(mSettings.mPackageName,
                        mSettings.mOnFormatCallback.formatClass(name)),
                mSettings.mOnFormatCallback.formatAttr(name)), name);
    }

    private FieldSpec handleArray(Type type, String name) {
        return finalizeAttr(FieldSpec.builder(ParameterizedTypeName.get(List.class, type),
                mSettings.mOnFormatCallback.formatAttr(name)), name);
    }

    private FieldSpec handleArray(TypeName typeName, String name) {
        return finalizeAttr(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), typeName),
                mSettings.mOnFormatCallback.formatAttr(name)), name);
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

    public void handleRootAsArray(JSONArray jsonArray) {
        handleRootAsArray(jsonArray, mSettings.mClassName);
    }

    private void handleRootAsArray(JSONArray jsonArray, String key) {
        Object firstItem = jsonArray.get(0);
        if (WRAPPER_TYPES.contains(firstItem.getClass()))
            // Primitive array type
            reportError("Root array with primitive types can't be parsed to a java-class");
        else if (firstItem instanceof JSONObject) {
            loopHandleRootAsObject((JSONObject)firstItem, String.format("%sObj", key));
        } else
            reportError("Unsupported key type: " + firstItem.getClass().getSimpleName());
    }

    private FieldSpec loopHandleRootAsObject(JSONObject jsonObject, String parentKey) {
        ArrayList<FieldSpec> fields = new ArrayList<>();
        ArrayList<MethodSpec> methods = new ArrayList<>();

        for (Object k : jsonObject.keySet()) {
            if (k instanceof String) {
                String key = (String) k;
                // Check if keyword is ignored
                if (mSettings.mIgnores.containsKey(key))
                    continue;

                FieldSpec field = null;
                Object obj = jsonObject.get(key);
                if (isNestedJson(obj)) {
                    if (obj instanceof JSONObject) {
                        // Handling the case of a nested json object
                        field = loopHandleRootAsObject((JSONObject) obj, key);
                    } else if (obj instanceof JSONArray) {
                        // Handling the case of a nested json array
                        Object firstItem = ((JSONArray) obj).get(0);
                        String listName = String.format("%ss", key);
                        if (WRAPPER_TYPES.contains(firstItem.getClass()))
                            // Primitive array type
                            field = handleArray(firstItem.getClass(), listName);
                        else if (firstItem instanceof JSONObject) {
                            FieldSpec spec = loopHandleRootAsObject((JSONObject)firstItem, key);
                            field = handleArray(spec.type, listName);
                        } else
                            reportError("Unsupported key type: " + firstItem.getClass().getSimpleName());
                    }
                } else
                    field = handleAttribute(obj.getClass(), key);

                if (field != null) {
                    fields.add(field);
                    applyMethods(methods, key, field.type);
                }
            } else
                reportError("Unsupported key type: " + k.getClass().getSimpleName());
        }
        handleClass(mSettings.mOnFormatCallback.formatClass(parentKey), fields, methods);
        return handleAttribute(parentKey);
    }

    private void applyMethods(ArrayList<MethodSpec> methods, String key, TypeName type) {
        // Adding getter and setter method if specified
        if (mSettings.mGetters.containsKey(key))
            methods.add(GetterSetter.generateGetter(key, type));
        if (mSettings.mSetters.containsKey(key))
            methods.add(GetterSetter.generateSetter(key, type));
    }

    private void reportError(String error) {
        mResultListener.onError(AutoJSON.TAG + error);
    }

    public void handleRootAsObject(JSONObject jsonObject) {
        loopHandleRootAsObject(jsonObject, mSettings.mClassName);
    }

    private static boolean isNestedJson(Object possibleJson) {
        return possibleJson instanceof JSONObject || possibleJson instanceof JSONArray;
    }

    private static JsonDecompiler ourInstance = null;
    static JsonDecompiler getInstance(AutoJSON.Builder settings, OnDecompileFinishedHandler result) {
        if (ourInstance == null)
            ourInstance = new JsonDecompiler(settings, result);
        return ourInstance;
    }
    private JsonDecompiler(AutoJSON.Builder settings, OnDecompileFinishedHandler result) {
        this.mSettings = settings;
        this.mResultListener = result;
    }
}
