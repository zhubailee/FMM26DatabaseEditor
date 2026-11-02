# DAT binary foundation

This package contains bounded little-endian primitive I/O helpers for investigating and parsing FMM Mobile `.dat` files.

## Safety and scope

- These helpers do not define a player, club, transfer, or competition record schema.
- String encoding is implemented as a 32-bit byte-length prefix followed by UTF-8, matching the inspected reference implementation.
- Record layouts, headers, version-specific mappings, and write compatibility must be verified against a complete FMM26 Mobile database before enabling database mutation.
- Always edit a copied database and retain the untouched original.

Target database currently tracked by the app: archive 2603.
