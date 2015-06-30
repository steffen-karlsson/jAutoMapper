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

    private AutoJSON.Builder mSettings;

    protected interface OnDecompileFinishedHandler {
        void onError(String error);
        void onFinished(JavaFile file);
    }

    private final Set<Class> WRAPPER_TYPES = new HashSet<Class>(Arrays.asList(
            Boolean.class, String.class, Integer.class, Long.class, Float.class, Double.class));

    private JavaFile handleJavaFile(TypeSpec classTypeSpec) {
        return JavaFile.builder(mSettings.mPackageName, classTypeSpec).build();
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

    public void handleRootAsArray(JSONArray jsonArray, OnDecompileFinishedHandler result) {
        //TODO: Handle array as root
    }

    private FieldSpec loopHandleRootAsObject(JSONObject jsonObject, String parentKey, OnDecompileFinishedHandler result) {
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
                    if (mSettings.mGetters.containsKey(key))
                        methods.add(GetterSetter.generateGetter(key, field.type));
                    if (mSettings.mSetters.containsKey(key))
                        methods.add(GetterSetter.generateSetter(key, field.type));
                }
            } else
                result.onError(AutoJSON.TAG + "Unsupported key type: " + k.getClass().getSimpleName());
        }

        TypeSpec clzz = handleClass(mSettings.mOnFormatCallback.formatClass(parentKey), fields, methods);
        result.onFinished(handleJavaFile(clzz));
        return handleAttribute(parentKey);
    }

    public void handleRootAsObject(JSONObject jsonObject, OnDecompileFinishedHandler result) {
        loopHandleRootAsObject(jsonObject, mSettings.mClassName, result);
    }

    private static boolean isNestedJson(Object possibleJson) {
        return possibleJson instanceof JSONObject || possibleJson instanceof JSONArray;
    }

    private static JsonDecompiler ourInstance = null;

    static JsonDecompiler getInstance(AutoJSON.Builder settings) {
        if (ourInstance == null)
            ourInstance = new JsonDecompiler(settings);
        return ourInstance;
    }
    private JsonDecompiler(AutoJSON.Builder settings) {
        this.mSettings = settings;
    }
}
