jAutoMapper
===========
##### Tired of writing `@SerializedName(...)` entity/data dummy `.java` class-files, in order to parse json API-calls, Take a look HERE!

`jAutoMapper` is a Java tool for generating `.java` class-files, based on either of following two conditions:
* A json-formattet string.
* URL to an API-call that returns json.

###Builder
An instance `AutoJSON.Builder` has following configurations and attributes:

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
- `shouldOverrideIfExists(...)`: Defines whether an already existing class should be overridden, default=`false`.
- `create()`: Last function to call, which returns an instance of `AutoJSON`.

###Compile
After specifying the configurations on the `AutoJSON.Builder` object, described above, is following functions available on the `AutoJSON` object:

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
AutoJSON conf = new AutoJSON.Builder()
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
    .addProperties(
        AttrProperty.ignore("country"),
        AttrProperty.generateGetterAndSetter("projectTitle"))
    .create();
    
// conf can now be called as many times, as there is API-calls with same configuration.
conf.buildJson(<Path to string>);
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
###Known bugs
- `Date.class`, `Calendar.class` support (at the moment they are decompiled as `String.class`).
- Only works for GET requests at the moment.


----

##Dependencies
- [JavaPoet][1]
- [OkHttp][2]
- [Google Gson][3]
- [Unirest][4]

##License
    The MIT License (MIT)

    Copyright (c) 2015 Steffen Karlsson
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.


 [1]: https://github.com/square/javapoet/
 [2]: https://github.com/square/okhttp/
 [3]: https://code.google.com/p/google-gson/
 [4]: https://github.com/Mashape/unirest-java
