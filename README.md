# KModules

A range of common effects functions, written for NodeProxy role filters

Note, this is strictly made according to my preferences so may not be very
useful for other people but go ahead and try!

Also: it's a work in progress :)

### Installation

In SuperCollider, evaluate the following code to install it as a quark:
`Quarks.install("https://github.com/madskjeldgaard/KModules.git");`

### Usage

```
// Load a buffer
b = Buffer.read(s, "path/to/soundfile.wav");

// Get a list of modules
k = KModules.new;

// Set a buffer player as source
Ndef('buf')[0] = k['src']['bufplayer1'];

// Set a buffer
Ndef('buf').set(\buffer, b);

// Play it
Ndef('buf').play;

// Add a random spectral fx!
Ndef('buf')[1] = \filter -> k['fx']['spectral'].choose;

// Add a second random spectral fx!!
Ndef('buf')[2] = \filter -> k['fx']['spectral'].choose;

// Add a third random spectral fx!!!
Ndef('buf')[3] = \filter -> k['fx']['spectral'].choose;

// Play around with the dry/wet parameters for each effect
Ndef('buf').set(\wet1, 0.5, \wet2, 0.25, \wet3, 0.66);
```
