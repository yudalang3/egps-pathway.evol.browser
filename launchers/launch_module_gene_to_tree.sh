#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$script_dir/_launch_module_common.sh" 'module.treebuilder.gene2tree.ModuleLoader4WebGene2TreeMain'
