# Features
+ Replace one text color with another (for every page in the document.)

# TODO
+ Perform multiple replacements (`original_color -> new_color`) in a single command.

# Limitations
+ Replacing colors must be done by specifying the target and replacement colors in the code. This should be done by reading a specification file.

# Usage
Within the `java` directory, run:

``` shell
./gradlew run <input-filepath> <output-filepath>
```
