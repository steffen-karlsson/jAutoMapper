jAutoMapper
===========

`jAutoMapper` is a Java tool for generating `.java` class-files, based on either of following two conditions:
* A json-formattet string.
* URL to an API-call that returns json.

###Features
The singleton instance `AutoJSON` has following features and attributes:
- `setClassName(...)`: The name of the outer most json object.
- `setOutDirectory(...)`: Directory of the generated `.java` class-files.
- `setPackageName(...)`: Package scope defined in each of the `.java` files.
- `addOnFormatAttributeNameCallback(...)`: Interface in order to get a callback, for specifying how to name attributes as well as classes.

    Default implementation of `OnFormatNameCallback`:
    ```java
    protected static OnFormatNameCallback mOnFormatCallback = new OnFormatNameCallback() {
        public String formatAttr(String orgName) {
           return orgName;
        }
        
        public String formatClass(String orgName) {
           return String.format("%s%s", orgName.substring(0, 1).toUpperCase(), 
                                orgName.substring(1));
        }
    };
    ```
- `addProperties(...)`: `Set` of `AttrProperty`'s for each of the attributes in the json file, for which one of following states holds:
    - `AttrProperty.ignore(...)`: Do not decompile attribute.
    - `AttrProperty.generateGetter(...)`: Generate getter for attribute.
    - `AttrProperty.generateSetter(...)`: Generate setter for attribute.
    - `AttrProperty.generateGetterAndSetter(...)`: Generate getter and setter for attribute.
- `setResultCallback(...)`: Callback to known when its successfully terminated or got canceled by an error.

###Compile
After specifying the configurations on the `AutoJSON` object, described above, can following functions be called:
- `buildJson(...)`: Building the `.java` class-files based on json string.
- `buildUrl(...)`: Building the `.java` class-files based on URL to an API that returns json.

###Example
This simple json string:
```json
{
    "projectTitle": "jAutoMapper is a Java-tool",
    "author": "Steffen Karlsson",
    "country": "Denmark",
    "JobTitle": "Developer (Android)",
    "company": {
        "name": "Shape A/S",
        "zip": 2300,
        "city": "Copenhagen"
    }
}
```
, which by following lines of code:
```java
AutoJSON.getInstance()
   .setClassName("Profile")
   .setResultCallback(new ResultCallback() {
      public void onSuccess() {
         System.out.println("Success");
      }
      
      public void onFailure() {
         System.out.println("Error");
      }
    })
    .setPackageName("com.sk.aj.example")
    .addProperties(new HashSet<AttrProperty>() {{
       add(AttrProperty.ignore("country"));
       add(AttrProperty.generateGetterAndSetter("projectTitle"));
    }})
    .buildJson(<Path to string>);
```
, can be translated into following two `.java` class-files:
####Profile.java
```java
package com.sk.aj.example;
    
import com.google.gson.annotations.SerializedName;
import java.lang.String;

public class Profile {

    @SerializedName("author")
    protected String author;
    
    @SerializedName("company")
    protected Company company;
    
    @SerializedName("JobTitle")
    protected String JobTitle;
    
    @SerializedName("projectTitle")
    protected String projectTitle;
    
    public String getProjectTitle() {
       return projectTitle;
    }
    
    public void setProjectTitle(String projectTitle) {
       this.projectTitle = projectTitle;
    }
}
```

####Company.java
```java
package com.sk.aj.example;

import com.google.gson.annotations.SerializedName;
import java.lang.Integer;
import java.lang.String;

public class Company {

    @SerializedName("zip")
    protected Integer zip;
    
    @SerializedName("name")
    protected String name;
    
    @SerializedName("city")
    protected String city;
}
```

