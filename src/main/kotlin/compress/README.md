# Compress

```
Usage: compress [<options>] <file>

  Pack files to decrease their size. Unpack files to retrieve the original.
  Packing file creates a packed file with same filename and .minzip extension.

  Files are compressed using Huffman encoding.

  ▎ On an empty file prints stderr error instead of compressing.

  ▎ If a file with .minzip suffix already exists during compression, prints
  ▎ error. Similarly, during decompression if a file without .minzip suffix
  ▎ already exists, prints error.

  Examples:
  ╭─────────────────────────────────╮
  │$ compress test.txt              │
  │test.txt.minzip (deflated to 10%)│
  ╰─────────────────────────────────╯
  ╭─────────────────────────────╮
  │$ compress -d test.txt.minzip│
  │test.txt (inflated to 1000%) │
  ╰─────────────────────────────╯

Options:
  -d, --decompress  If specified, file will be unpacked.
  -h, --help        Show this message and exit

Arguments:
  <file>  File to compress/unpack.
```