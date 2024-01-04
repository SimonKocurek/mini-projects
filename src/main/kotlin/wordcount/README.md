# Wordcount

```bash
Usage: wordcount [<options>] [<file>]

  Word, line, and byte or character count. Outputs result in format "<lines>
  <words> <chars or bytes> <file path>\n"

  Examples:
  ╭───────────────────────╮
  │$ wordcount -l test.txt│
  │1234 test.txt          │
  ╰───────────────────────╯
  ╭──────────────────────────╮
  │$ cat test.txt | wordcount│
  │1234 5678 9123            │
  ╰──────────────────────────╯

Options:
  -c, --bytes       Print the byte counts.
  -w, --words       Print the word counts.
  -l, --lines       Print the newline counts.
  -m, --chars       Print the character counts (assuming current locale).
  --charset=<text>  Use specific charset to count characters and words. System
                    default is used when not specified. Examples: US-ASCII,
                    UTF-8, UTF-16.
  -h, --help        Show this message and exit

Arguments:
  <file>  A pathname of an input file. If no file operands are specified, the
          standard input shall be used.
```