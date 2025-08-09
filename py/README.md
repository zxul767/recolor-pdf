# "Compilation"
`make` is equivalent to `make lint` and `make typecheck`:

# Code Formatting and Linting
`make lint`

# Code Typechecking
`make typecheck`


# Run Application
`make run ARGS="<input-filepath> <output-filepath>"`

The colors file specification is a standard JSON file like this one:

``` json
[
  {
    "target": "#FF0000",
    "replacement": "#0000FF"
  },
  {
    "target": "#00FF00",
    "replacement": "#802020"
  }
]
```
