package com.sk.aj.json;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sk.aj.AttrProperty;
import com.sk.aj.OnFormatNameCallback;
import com.sk.aj.ResultCallback;
import com.squareup.javapoet.JavaFile;
import com.sun.istack.internal.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by steffenkarlsson on 6/28/15.
 */
public class AutoJSON {

    protected final static String TAG = "AutoJSON: ";

    public AutoJSON.Builder mSettings;

    /**
     * Building the associated java classes based on json string.
     * @param json
     */
    public void buildJson(@NotNull String json) {
        verify();
        handleJson(json);
    }

    /**
     * Building the associated java classes based on URL to an API that returns json.
     * @param url
     */
    public void buildUrl(@NotNull URL url) {
        verify();
        downloadJson(url.toString());
    }

    /**
     * Building the associated java classes based on string url to an API that returns json.
     * @param url
     */
    public void buildUrl(@NotNull String url) {
        verify();
        downloadJson(url);
    }

    private void downloadJson(String url) {
        try {
            HttpResponse<String> response = Unirest.get(url)
                    .header("accept", "application/json")
                    .asString();
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                handleJson(response.getBody());
                return;
            }
        } catch (UnirestException ignore) {
        }
        if (mSettings.mResultCallback != null) {
            mSettings.mResultCallback.onFailure();
        }
    }

    private void handleJson(String json) {
        JsonNode node = new JsonNode(json);
        JsonDecompiler decompiler = JsonDecompiler.getInstance(mSettings, onDecompileFinishedHandler);
        if (node.isArray()) {
            decompiler.handleRootAsArray(node.getArray());
            return;
        }
        decompiler.handleRootAsObject(node.getObject());
    }

    private JsonDecompiler.OnDecompileFinishedHandler onDecompileFinishedHandler
            = new JsonDecompiler.OnDecompileFinishedHandler() {
        public void onError(String error) {
            System.err.println(TAG + "An error occurred during decompiling json to java files: " + error);
        }

        public void onFinished(JavaFile file) {
            try {
                file.writeTo(mSettings.mOut);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private void verify() {
        System.out.println(TAG + "Using class name: " + mSettings.mClassName);
        System.out.println(TAG + "Using package name: " + mSettings.mPackageName);

        if (mSettings.mResultCallback == null)
            System.out.println(TAG + "No ResultCallback defined");
        if (mSettings.usingDefaultFormatter)
            System.out.println(TAG + "Using default attribute name formatter, where json attribute name = java attribute name");

        if (mSettings.mOut == null) {
            mSettings.mOut = new File("out/");
            System.out.println(TAG + "Using default location: " + mSettings.mOut.getAbsolutePath());
        }

        if (!mSettings.mOut.exists())
            mSettings.mOut.mkdir();
    }

    private AutoJSON(AutoJSON.Builder builder) {
        mSettings = builder;
    }

    public static class Builder {
        private ResultCallback mResultCallback = null;
        private File mOut;
        private boolean usingDefaultFormatter = true;

        protected Map<String, AttrProperty> mIgnores = new HashMap<>();
        protected Map<String, AttrProperty> mGetters = new HashMap<>();
        protected Map<String, AttrProperty> mSetters = new HashMap<>();

        protected String mClassName = "Out";
        protected String mPackageName = "com.example.entities";
        protected OnFormatNameCallback mOnFormatCallback = new OnFormatNameCallback() {
            public String formatAttr(String orgName) {
                return orgName.toLowerCase();
            }

            public String formatClass(String orgName) {
                return String.format("%s%s", orgName.substring(0, 1).toUpperCase(), orgName.substring(1));
            }
        };


        /**
         * Attach a {@link ResultCallback} in order to known when its
         * successfully terminated or got canceled by an error.
         * @param resultCallback
         */
        public AutoJSON.Builder setResultCallback(@NotNull ResultCallback resultCallback) {
            this.mResultCallback = resultCallback;
            return this;
        }

        /**
         * Define the output directory of the java files generated
         * @param out
         */
        public AutoJSON.Builder setOutDirectory(@NotNull File out) {
            this.mOut = out;
            return this;
        }

        /**
         * Sets the name of class of the outer most json object by {@code className}.
         * @param className
         */
        public AutoJSON.Builder setClassName(@NotNull String className) {
            mClassName = className;
            return this;
        }

        /**
         * Sets the package name of which the generated classe(s) will be defined.
         * @param packageName
         */
        public AutoJSON.Builder setPackageName(@NotNull String packageName) {
            mPackageName = packageName;
            return this;
        }

        /**
         * Add an array of {@link AttrProperty}, where each has one of following states:<br>
         * <ul>
         *     <li>AttrProperty.ignore: Do not decompile attribute</li>
         *     <li>AttrProperty.generateGetter: Generate getter for attribute</li>
         *     <li>AttrProperty.generateSetter: Generate setter for attribute</li>
         *     <li>AttrProperty.generateGetterAndSetter: Generate getter and setter for attribute</li>
         * </ul>
         * Example implementation:
         * <pre>
         * <code>AutoJSON.getInstance().addProperties(new HashSet<AttrProperty>() {{
         *    add(AttrProperty.ignore("name"));
         *    add(AttrProperty.ignore("age"));
         *    add(AttrProperty.generateGetterAndSetter("title"));
         * }});
         *
         * </code>
         * </pre>
         * @param properties
         */
        public AutoJSON.Builder addProperties(@NotNull AttrProperty... properties) {
            for (AttrProperty property : properties) {
                String name = property.getName();
                if (property.shouldIgnore()) {
                    mIgnores.put(name, property);
                    continue;
                }
                if (property.shouldGenerateGetter())
                    mGetters.put(name, property);
                if (property.shouldGenerateSetter())
                    mSetters.put(name, property);
            }
            return this;
        }

        /**
         * Attach a {@link OnFormatNameCallback} interface in order, to get a callback for refactoring when an
         * attribute ({@code formatAttr}) or a class ({@code formatClass}) needs to be associated with a name.
         * <br/><br/>
         *
         * Default implementation {@link OnFormatNameCallback}:
         * <pre>
         * <code>public String formatAttr(String orgName) {
         *    return orgName;
         * }
         *
         * public String formatClass(String orgName) {
         *    return String.format("%s%s", orgName.substring(0, 1).toUpperCase(), orgName.substring(1));
         * }
         * </code>
         * </pre>
         * @param formatCallback
         */
        public AutoJSON.Builder addOnFormatAttributeNameCallback(@NotNull OnFormatNameCallback formatCallback) {
            mOnFormatCallback = formatCallback;
            this.usingDefaultFormatter = false;
            return this;
        }

        public AutoJSON create() {
            return new AutoJSON(this);
        }
    }
}
