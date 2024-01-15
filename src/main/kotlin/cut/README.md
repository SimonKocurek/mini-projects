# Cut

```
Usage: cut [<options>] [<file>]

  Cut out selected portions of each line of a file.

  Outputs result in format "<line1 field1><delimiter><line1 field2>\n<line2
  field1><delimiter><line2 field2>\n"

  ▎ If enough delimited fields are not found in the line, whole line is printed
  ▎ out.

  Examples:
  ╭─────────────────────────────╮
  │$ cut -d : -f 1,7 /etc/passwd│
  │nobody:/usr/bin/false        │
  │root:/bin/sh                 │
  ╰─────────────────────────────╯
  ╭───────────────────────╮
  │$ who | cut -f 6 -d ' '│
  │console                │
  │ttys000                │
  ╰───────────────────────╯

Options:
  -f, --fields=<text>     List of indexes of fields (delimited by -d option)
                          that should be printed out. Fields are indexed
                          starting with 1. Example -f '1,2'
  -d, --delimiter=<text>  Delimiter character to split fields. Delimiter is
                          also used to separate fields in the output. Default:
                          TAB.
  -h, --help              Show this message and exit

Arguments:
  <file>  A pathname of an input file. If no file operands are specified, the
          standard input shall be used.
```