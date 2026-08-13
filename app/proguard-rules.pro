-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepattributes Signature,Exceptions,*Annotation*

-keepclassmembers,allowobfuscation enum ** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.annotations.**