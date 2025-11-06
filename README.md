# btw-presence-footsteps
Port of the modloader version of Presence Footsteps, integrated with Better Than Wolves

## Installation
1. Add the latest version of the mod to your BTW instance's ``mods/`` folder
2. Run the instance once, let it generate the ``presencefootsteps/`` directory in your ``mods/`` folder
3. Download the resource archive from the latest version and extract its contents into ``presencefootsteps/packs/``
4. Restart the game, or reload your resources via ``CTRL+P`` in order to hear PF in action!

## Usage
* F9: Opens volume mixer
* CTRL+P: Hot reloads only BTWPF resources 
* CTRL+SHIFT+F: Enables verbose logging

## About PF Packs
PF packs consist of a resource pack component with audio data, and audio definitions that 
define what context they play in. The ``user.packname.r0`` field in the ``userconfig.cfg`` file can be renamed
to point to any PF pack within ``presencefootsteps/``. Outside of that, the most important files are:
* ``acoustics.json``: Maps the sounds within the PF resource pack (or sounds registered by Minecraft/addons) to various contexts
* ``blockmap.cfg``: Maps acoustic definitions to specific block IDs, metadata, and collision types (foliage, carpet)
* ``primitivemap.cfg``: Maps acoustic definitions to block materials

Namespace must be provided if you're targeting sounds or materials from a specific mod/addon. e.g. ``btw@stone`` 
in the case of a ``primitivemap`` material def, and ``@btw:block.stone.break`` in the case of an ``acoustics``
name field. Other usage beyond this can be inferred with the provided resource set and definitions. Verbose logging
will tell you how definitions are being parsed, and the cases where there are no block or primitive definitions 
properly set.

## License

This template is available under the WTFPLv2 license. Do whatever you please.
This project incorporates:
* A precompiled version of [Tiny Remapper](https://github.com/FabricMC/tiny-remapper) (LGPL-3.0)
