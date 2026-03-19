# Newick Format Reference

## Overview

`nwk.format` controls how Newick node text is interpreted by the phylogenetic tree importer. The parameter is exposed in `ParamsAssignerAndParser4PhyloTree` and consumed by the tree parser used by Modern Tree Viewer and related tree modules.

## Supported Formats


| Format | Leaf Node            | Internal Node           | Meaning                                |
| ------ | -------------------- | ----------------------- | -------------------------------------- |
| `0`    | `name:dist`          | `support:dist`          | Flexible format with support values    |
| `1`    | `name:dist`          | `name:dist`             | Internal node names instead of support |
| `2`    | `name:dist` (strict) | `support:dist` (strict) | Strict support-value format            |
| `3`    | `name:dist` (strict) | `name:dist` (strict)    | Strict node-name format                |
| `4`    | `name:dist`          | none                    | Leaf-only internal annotations removed |
| `5`    | `name:dist`          | `:dist` only            | Internal nodes keep distance only      |
| `6`    | `name:dist`          | `name` only             | Internal nodes keep name only          |
| `7`    | `name` only          | `name` only             | No distances                           |
| `8`    | `name` only          | none                    | Leaf names only                        |
| `9`    | none                 | none                    | Pure topology                          |

## Support Value Convention

Bootstrap/support values should stay in the `0-100` range.


| Correct | Incorrect |
| ------- | --------- |
| `90`    | `0.9`     |
| `95`    | `0.95`    |
| `100`   | `1.0`     |

## Current Implementation

- `ParamsAssignerAndParser4PhyloTree.java` defines `nwk.format`.
- `TreeParser4Evoltree.java` reads the selected format during parsing.
- `manual_en.html` and `manual_zh.html` document the supported values for users.


## References

- [ETE3 Newick Parser Source](https://github.com/etetoolkit/ete/blob/ete3/ete3/parser/newick.py)
- [ETE Toolkit Tutorial](https://etetoolkit.org/docs/latest/tutorial/tutorial_trees.html)
- [NHX Format Specification](http://www.phylosoft.org/NHX/)
- [Newick Format Wikipedia](https://en.wikipedia.org/wiki/Newick_format)