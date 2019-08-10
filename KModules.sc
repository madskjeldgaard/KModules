/*


TODO: 
- Add 1-64 channel versions of everything

*/
KModules{
	classvar <>moduleList,
	<>holyControls, <>numchans=2;

	//this class is run during library compile time.
	*initClass{

		//this method makes sure that the IDict is compiled before
		//trying to make an instance of it
		Class.initClassTree(IdentityDictionary);

		moduleList = IdentityDictionary.new;

		holyControls = [\start, \buffer, \rate, \interpolation, \amp, \cbuffer, \crate];

		this.loadDefaultKModules;

	}

	*new{ |numChans=2|
		this.loadDefaultKModules;

		numchans = numChans;

		this.addSynthDefs;

		^moduleList;
	}

	*addToModuleList{|modulesToAdd|
		// TODO
		^moduleList.putAll(modulesToAdd.asEvent);
	}

	*addSynthDefs{
		moduleList.keysValuesDo{|subcat, subcatContent|
			// Modules
			if(subcatContent.size > 0, {
				subcatContent.keysValuesDo{|moduleName, moduleContent|
					SynthDef((moduleName ++ numchans).asSymbol, { |in, out, wet=0.5|
						var insig = In.ar(in, numchans);
						var sig = SynthDef.wrap(moduleContent, prependArgs: [insig]);

						// TODO: Make mono + multi chan compatible
						sig = XFade2.ar(insig, sig, wet.linlin(0.0,1.0,-1.0,1.0));

						ReplaceOut.ar(out, sig);
					}).add;
				}
			})
		}
	}

	*postModuleList{
		// Categories
		this.lineBreak;
		moduleList.keysValuesDo{|subcat, subcatContent|

			// Modules
			if(subcatContent.size > 0, {
				subcatContent.keysValuesDo{|moduleName, moduleContent|
					( "\t\t\t" ++ moduleName).postln;
					moduleContent.argNames.do{|a| "\t\t\t\targ: %".format(a).postln};

				}
			})
		}
	}

	*loadDefaultKModules{

		this.addToModuleList( 
			(
				pitch: (
					pitchshift: {|in, grain=0.25, pitch=1, pd=0, td=0|
						PitchShift.ar(in, 0.001 + grain, 0.01 + (pitch*4), pd, td)
					},

					ranpitchshift: {|in, grain=0.25, pitch=1, pitchspread=0.0|
						var numpitchs=5;
						var rp = {rrand(0.1, 4.0)}!numpitchs;
						var rg = {rrand(0.001, 1.0)}!numpitchs;
						var rpd = {rrand(0.001, 1.0)}!numpitchs;
						var rtd = {rrand(0.001, 1.0)}!numpitchs;

						Splay.ar(
							PitchShift.ar(in, 0.001 + grain, rp * pitch*4, rpd, rtd),
							pitchspread
						)
					},
				),
				time: (
					freqshift: {|in, freq=0.1, phase=0|
						FreqShift.ar(in, freq*2, phase.linlin(0.0, 1.0, -2pi, 2pi))
					},

					delay: {|in, delay=1|
						CombC.ar(in, 4, 0.1 + (delay * 2).clip(0.0, 2.0), 0.1 + (delay*3)).tanh
					},

					reverb1: {|in, damp=0.15, size=0.25|
						FreeVerb.ar(in, 1.0, size, damp);
					},

					reverb2: {|in, damp=0.15, size=0.25|
						FreeVerb2.ar(in[0], in[1], 1.0, size, damp);
					},
				),
				destruction: (
					ringmod: {|in, carfreq=0.1|
						DiodeRingMod.ar(
							SinOsc.ar(carfreq.linexp(0.0,1.0, 20.0, 12000),
							in)
						)
					},

					waveloss: {|in, drop=0.5|
						WaveLoss.ar(in, drop * 40, 40, 2)
					},
				),
				physical: (

				),
				filter: (
					klank1: {|in, klankf1=0.01, klankf2=0.1, klankf3=0.5, klankf4=0.33|

						DynKlank.ar(`[
							[
								klankf1.linexp(0.0,1.0, 40.0, 12000),
								klankf2.linexp(0.0,1.0, 40.0, 12000),
								klankf3.linexp(0.0,1.0, 40.0, 12000),
								klankf4.linexp(0.0,1.0, 40.0, 12000)],
								Array.rand(4, 0.1, 2.0),
								[1, 1, 1, 1]
							],
							in).tanh
						},

						klank2: {|in, klankf1=0.01, klankf2=0.1, klankf3=0.5, klankf4=0.33|

							DynKlank.ar(`[
								[
									klankf1.linexp(0.0,1.0, 40.0, 12000),
									klankf2.linexp(0.0,1.0, 40.0, 12000),
									klankf3.linexp(0.0,1.0, 40.0, 12000),
									klankf4.linexp(0.0,1.0, 40.0, 12000)],
									0.001!4,
									Array.rand(4, 0.1, 2.0)
								],
								in).tanh!2

							}
						),
						granular: (
							warpin: {|in, overlaps=0.5, windowSize=0.5, warprate=0.01, windowRand=0.01, warpScale=0.5, buffer|
								var bufLength = 2.0;

								var chain = LocalBuf(bufLength * SampleRate.ir, in.size).clear;

								var pointer = Phasor.ar(0, SampleDur.ir / BufDur.ir(chain) * warprate.linexp(0.0,1.0, 0.001, 100.0)); 

								RecordBuf.ar(in, chain);

								Warp1.ar(
									numChannels: in.size, 
									bufnum: chain, 
									pointer: pointer, 
									freqScale: warpScale.linlin(0.0,1.0, 0.01, 10.0),
									windowSize: windowSize.linlin(0.0,1.0, 0.001, 5.0), 
									envbufnum: -1, 
									overlaps: overlaps.linlin(0.0,1.0,1,16),
									windowRandRatio: windowRand, 
									interp: 4, 
									mul: 1, 
									add: 0
								);
							}
							fbgrain: {|in, fbgrainsize = 0.25, fbgrainrand = 0.8, fbGain = 0|

								/*
								This one was stolen from David Granström's SuperPrism project,
								another big inspiration: github.com/davidgranstrom/SuperPrism
								*/

								var bufLength = 1.0;
								var localBuf = LocalBuf(bufLength * SampleRate.ir, in.size).clear;

								var warp = Warp1.ar(
									in.size,
									localBuf,
									LFSaw.ar(1/bufLength).linlin(-1.0,1.0,0.0, 1.0),
									Drand([ 2, - 2 ], inf),
									fbgrainsize.linlin(0.0, 1.0, 0.0, 2.0),
									-1,
									2,
									fbgrainrand.linlin(0.0, 1.0, 0.2, 1.0),
									4
								);

								// record w/ feedback
								RecordBuf.ar(tanh(in + HPF.ar(tanh(warp * fbGain), 30)), localBuf);

								warp = warp.tanh;
								warp = HPF.ar(warp * 0.5, 150);
								warp.sanitize;
							},
						),
						spectral: (
							spectraldelay: {|in, tsdelay=0.8, xsdelay = 1|

								var signal, delayTime, delays, freqs, filtered;
								var size = 32;
								var maxDelayTime = 1.0;

								delayTime = tsdelay * maxDelayTime;

								(1..size).sum{ |i|
									var filterFreq = i.linexp(1, size, 40, 17000);
									var sig = BPF.ar(in, filterFreq, 0.005);
									// the delay pattern is determined from xsdelay by bitwise-and:
									DelayN.ar(sig, maxDelayTime, i & xsdelay * (1/size) * delayTime )
								};

							},

							freeze: { |in, freeze=1|

								var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

								IFFT(PV_Freeze(chain, freeze)); 

							},

							smear: {|in, smear=0.5|

								var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

								IFFT(PV_MagSmear(chain, bins: smear*100)); 

							},

							scramble: {|in, scramwipe=0.5, scramwidth=0.5|

								var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

								IFFT(PV_BinScramble(chain, wipe: scramwipe, width: scramwidth)); 

							},        

							conformer: {|in, areal=0.5, aimag=0.5|

								var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

								chain = PV_ConformalMap(chain, areal.linlin(0.0,1.0,0.01,2.0), aimag.linlin(0.00,1.0,0.01,10.0));

								IFFT(chain);

							},

							enhance: {|in, numPartials=0.5, ratio=0.25, strength=0.3|
								var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

								chain = PV_SpectralEnhance(chain, 
									numPartials.linlin(0.0,1.0, 1, 16),
									ratio.linlin(0.0,1.0,1.0,5.0), 
									strength.linlin(0.0,1.0,0.0,0.99));

									IFFT(chain);
								},

								comb: {|in, teeth=0.5, comb=0.5|

									var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

									IFFT(PV_RectComb(chain, numTeeth: teeth*comb, width:1-comb)); 

								},

								binshift: {|in, binshift=0.5|

									var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

									IFFT(PV_BinShift(chain, stretch: binshift.range(0.01,4), shift: binshift.range(0,10), interp:0)); 

								},

								hbrick: {|in, hbrick=0.9|

									var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

									IFFT(PV_BrickWall(chain, wipe:hbrick)); 

								},

								lbrick: {|in, lbrick=0.0|

									var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

									IFFT(PV_BrickWall(chain, wipe:lbrick.linlin(0.0,1.0,0.0,-1.0))); 

								},

								diffuser: {|in, diffuse=0.2|

									var chain = Array.fill(in.size, {|i| FFT(LocalBuf(2048), in[i])});	

									IFFT(PV_Diffuser(chain, Dust.kr(diffuse.linlin(0.0,1.0,0.0,10.0)))); 

								},

							)

						)
					)
				}

				// Cute random line break
				*lineBreak{|length=40|
					var linechars = ['~', '-', '_', '`', ',', '.', '^', '´'];

					var c = linechars.choose;

					"\n".post;

					length.do{ c.post};

					"\n".post;

				}

			}
