# Compress

```
Usage: compress [<options>] <file>

  Pack files to decrease their size. Unpack files to retrieve the original.

  Files are packed using Huffman encoding.

  ▎ On an empty file outputs stderr error instead of processing.

  ▎ If a file with name specified in --output argument already exists, outputs
  ▎ error and stops.

  Examples:
  ╭─────────────────────────────────╮
  │$ compress test.txt              │
  │test.txt.minzip (deflated to 10%)│
  ╰─────────────────────────────────╯
  ╭──────────────────────────────────────────────────────╮
  │$ compress --output test.json --unpack test.txt.minzip│
  │test.json (inflated to 1000%)                         │
  ╰──────────────────────────────────────────────────────╯

Options:
  -u, --unpack         If specified, file will be unpacked.
  -o, --output=<text>  Name of the generated file. If no name is provided,
                       '.minzip' will be added/removed from the input file.
  -h, --help           Show this message and exit

Arguments:
  <file>  File to pack/unpack.
```