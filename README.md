# OddGuard
## My Worldguard alternative.
### Why did you make an alternative to a plugin that works completly fine?

On my purpur server, for whatever reason, it just refuses to work. It may be a plugin conflict or something, but on 1.20.6 purpur I could not get Worldguard to work. Yes, I tried the latest alpha build after the latest release one failed.

### Okay, so how does this work?
You can make select a region with the /og wand (left click for position 1, right click for position 2), then running /og create <region name>. This will create the region with the specified name. You can edit the region's flags, or what is allowed in the region, using the GUI /og flags while standing in the region, or /og flags <region name> to specify a region name. You can also manually write to flags using /og flag <region name> <flag> <true / false / unset>

### Woah, how do I use this GUI?

You can left click any of the flags to toggle true / false, or right click to unset. If you unset, the flag will behave like normal. If the flag is true, it will remain true in the region. True has precidence over false, so if you have overlapping regions with one of them having block-break set to true, but another has one set to false, the player will be allowed to place blocks inside the true region, even if it overlaps with the false region. You can stop this by setting the flag to unset, which will act like the flag doesnt exist, so any other overlapping region will set it.

### Okay, but what about my builders? How can I protect my server while letting my builders build?

You can bypass OddGuard using /og bypass with the permission node oddguard.bypass. This will bypass all region restrictions.

### I noticed that you save the regions inside config.yml. Is there any reason for this?

The reason is that using the config.yml is the easiest way to code it for me.
