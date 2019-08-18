KModules {
	var moduleList, synthdefs, ctkprotonotes, quarkpath, modulenames;

	//this class is run during library compile time.
	*initClass{

		//this method makes sure that the IDict is compiled before
		//trying to make an instance of it
		Class.initClassTree(IdentityDictionary);

	}

	init{ |numchans|
		var loadedfxmodules;

		quarkpath = Quark("KModules").localPath;

		// Load file with fx modules
		// var loadedfxmodules = (quarkpath +/+ "fx" +/+ "fxmodules.scd").load;
		loadedfxmodules = this.loadFile();

		loadedfxmodules = loadedfxmodules.value(numchans); // Returns an event

		synthdefs = [];
		ctkprotonotes = CtkProtoNotes.new;
		modulenames = Array.new;
		moduleList = IdentityDictionary.new;
		moduleList.putAll(loadedfxmodules);

		// Make and add synth defs from the fx functions
		this.makeSynthDefs(numchans);
		this.addSynthDefs();
		this.addCtkDefs();

		// Gather module names
		loadedfxmodules.isNil.not(
			this.crawl({|synthname, synthfunc| modulenames = modulenames.add(synthname)});
		);


		^this;
	}

	*new{ |numchans=2|
		^super.new.init(numchans)
	}

	crawl{ |doFunction|
		moduleList.keysValuesDo{|subcat, subcatContent|
			// Modules
			if(subcatContent.size > 0, {
				subcatContent.keysValuesDo{|moduleName, moduleContent|
					doFunction.value(moduleName, moduleContent)	
				}
			})
		}
	}

	addSynthDefs{
		synthdefs.do{ |def|

			// Add the synth to the system
			def.add;

		}
	}

	addCtkDefs{
		synthdefs.do{|def|
			ctkprotonotes = ctkprotonotes.add(def);
		}
	}

	modules{
		^moduleList
	}

	defs{
		^synthdefs
	}

	names{
		^modulenames
	}

	asCtkProtoNotes{
		^ctkprotonotes	
	}

	postModuleList{
		// Categories
		this.lineBreak;

		this.crawl({|moduleName, moduleContent|
			( "\t\t\t" ++ moduleName).postln;
			moduleContent.argNames.do{|a| "\t\t\t\targ: %".format(a).postln};
		})
	}

	// Cute random line break
	lineBreak{|length=40|
		var linechars = ['~', '-', '_', '`', ',', '.', '^', '´'];

		var c = linechars.choose;

		"\n".post;

		length.do{ c.post};

		"\n".post;

	}
}

// Manage fx synths
Kfx : KModules {
	loadFile{
		^(quarkpath +/+ "modules" +/+ "fxmodules.scd").load;
	}

	makeSynthDefs{|numchans|
		var makesynth = {|moduleName, moduleContent|

			// Create synthdef
			var def = SynthDef((moduleName ++ numchans).asSymbol, { |in, out, wet=1.0|
				var insig = In.ar(in, numchans);
				var sig = SynthDef.wrap(moduleContent, prependArgs: [insig]);

				// TODO: Make mono + multi chan compatible
				sig = XFade2.ar(insig, sig, wet.linlin(0.0,1.0,-1.0,1.0));

				ReplaceOut.ar(out, sig);
			});

			// Add to global synthdef array of instance
			synthdefs = synthdefs.add(def);

		};

		this.crawl(makesynth)
	}
}

// Manage source synths
Ksrcs : KModules {
	loadFile{
		^(quarkpath +/+ "modules" +/+ "srcmodules.scd").load;
	}

	makeSynthDefs{|numchans|
		var makesynth = {|moduleName, moduleContent|

			// Create synthdef
			var def = SynthDef((moduleName ++ numchans).asSymbol, { |out, amp=0.1|
				var sig = SynthDef.wrap(moduleContent);

				OffsetOut.ar(out, sig*amp);
			});

			// Add to global synthdef array of instance
			synthdefs = synthdefs.add(def);

		};

		this.crawl(makesynth)
	}
}
