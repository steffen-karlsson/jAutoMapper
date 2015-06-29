package com.sk.aj;

/**
 * Created by steffenkarlsson on 6/29/15.
 */
final public class AttrProperty {

    private String mName;
    private boolean shouldGenerateGetter = false;
    private boolean shouldGenerateSetter = false;
    private boolean shouldIgnore = false;

    private AttrProperty(String name, boolean shouldGenerateGetter, boolean shouldGenerateSetter) {
        this.mName = name;
        this.shouldGenerateGetter = shouldGenerateGetter;
        this.shouldGenerateSetter = shouldGenerateSetter;
    }

    private AttrProperty(String name, boolean ignore) {
        this.mName = name;
        this.shouldIgnore = ignore;
    }

    public static AttrProperty generateSetter(String name) {
        return new AttrProperty(name, false, true);
    }

    public static AttrProperty generateGetter(String name) {
        return new AttrProperty(name, true, false);
    }

    public static AttrProperty generateGetterAndSetter(String name) {
        return new AttrProperty(name, true, true);
    }

    public static AttrProperty ignore(String name) {
        return new AttrProperty(name, true);
    }

    public boolean shouldGenerateSetter() {
        return shouldGenerateSetter;
    }

    public boolean shouldGenerateGetter() {
        return shouldGenerateGetter;
    }

    public boolean shouldIgnore() {
        return shouldIgnore;
    }

    public String getName() {
        return mName;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AttrProperty && ((AttrProperty) obj).mName.equalsIgnoreCase(mName);
    }
}
