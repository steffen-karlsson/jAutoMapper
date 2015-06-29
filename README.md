jAutoMapper
===========

`jAutoMapper` is a Java tool for generating `.java` class-files, based on either of following two conditions:
* A json-formattet string.
* URL to an API-call that returns json.

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

