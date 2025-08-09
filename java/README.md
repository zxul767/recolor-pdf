# Code Formatting
`./gradlew spotlessApply`

# Run Application
`./gradlew run <input-filepath> <output-filepath> <colors-filepath>`

The colors file specification is a standard Java properties file like this one:

``` java-properties
# colors must be specified in hex notation (without a leading # character)
# target-color=replacement-color
FF0000=0000FF
00FF00=802020
```
