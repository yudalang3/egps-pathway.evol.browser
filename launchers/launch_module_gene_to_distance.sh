#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$script_dir/_launch_module_common.sh" 'module.evoldist.gene2dist.ModuleLoader4WebGene2EvolDistMain'
