// TODO: Make sure all fx are multi channel ready
{|numchans|
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
			// Ring modulation stolen from SuperDirt. ring=modilation amount, ringf=modfreq,ringdf=slide in modfreq
			dirtring: { |in, ring = 0.5, ringf = 0.5, ringdf=0.5|
				var signal, mod;
				signal = in;
				ringf = ringf.linexp(0.0,1.0,1.0,20000.0);
				mod = ring * SinOsc.ar(Clip.kr(XLine.kr(ringf, ringf + ringdf), 20, 20000));
				ring1(signal, mod);
			},
			// Taken from Thor Magnussons book Scoring Sound: https://leanpub.com/ScoringSound/read#leanpub-auto-flanger 
			flanger: { |in, flangdelay=0.1, flangdepth=0.08, flangrate=0.06, flangfb=0.01, decay=0.0|
				var input, maxdelay, maxrate, dsig, mixed, local;
				maxdelay = 0.013;
				maxrate = 10.0;
				input = in;
				local = LocalIn.ar(1, numchans);

				// the delay (you could use AllpassC (put 0 in decay))
				dsig = AllpassC.ar( 
					input + (local * flangfb),
					maxdelay * 2,
					// very similar to SinOsc (try to replace it) - Even use LFTri
					LFPar.kr( 
						flangrate * maxrate,
						0,
						flangdepth * maxdelay,
						flangdelay * maxdelay
					),
					decay
				);

				mixed = input + dsig;
				LocalOut.ar(mixed);
				mixed;
			}, 
			// Taken from Thor Magnussons book Scoring Sound: https://leanpub.com/ScoringSound/read#leanpub-auto-chorus 
			chorus: { |in, chpredelay=0.08, chrate=0.05, chdepth=0.1, chphasediff=0.5|
				var sig, modulators, numDelays = 12;
				in = in * numDelays.reciprocal;
				modulators = Array.fill(numDelays, {arg i;
					LFPar.kr(chrate * rrand(0.94, 1.06), chphasediff * i, chdepth, chpredelay)}
				); 
				sig = DelayC.ar(in, 0.5, modulators);  
				numDelays.reciprocal * sig.sum!numchans;
			},

			freqshift: {|in, freq=0.1, phase=0|
				FreqShift.ar(in, freq*2, phase.linlin(0.0, 1.0, -2pi, 2pi))
			},

			// Phaser taken from SuperDirt
			// https://github.com/musikinformatik/SuperDirt/blob/develop/synths/core-synths.scd
			phaser: { |in, phaserrate = 1.0, phaserdepth = 0.5|
				var signal, phaserwave;
				var phase =  LFPar.kr(phaserrate).range(0.0088, 0.01);

				phaserdepth = phaserdepth.clip(0, 1);

				signal = in;

				phaserwave = AllpassL.ar(signal, 4, phase, 0, phaserdepth);

				(signal + phaserwave)/2;
			},

			// Also taken from SuperDirt
			shaper: { |in, shape = 0|
				var signal = in;
				shape = shape.clip(0.0,0.90);

				shape = min(shape, 1.0 - 4e-10); // avoid division by zero
				//amp = 1.0 - (0.15 * shape / (shape + 2.0)) * amp; // optional gain comp
				shape = (2.0 * shape) / (1.0 - shape);

				LeakDC.ar((1 + shape) * signal / (1 + (shape * abs(signal))));
			},

			// Also taken from SuperDirt
			bitcrush: { |in, crush = 0.1|
				var signal = in;
				signal.round(0.5 ** (crush.linlin(0.0,1.0,16,1)-1));
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
					in
				).tanh
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
					in
				).tanh!2

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
			},
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
				}.dup(numchans);
			},

			freeze: {|in, freeze=1|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				in.poll;
				IFFT(PV_BinScramble(chain, 0.5, 0.85)); 
			},

			// convolution: {|in|}, // TODO

			smear: { |in, smear=0.5|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				IFFT(PV_MagSmear(chain, bins: smear*100)); 

			},

			scramble: {|in, scramwipe=0.5, scramwidth=0.5|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				IFFT(PV_BinScramble(chain, wipe: scramwipe, width: scramwidth)); 

			},        

			conformer: {|in, areal=0.5, aimag=0.5|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				chain = PV_ConformalMap(chain, areal.linlin(0.0,1.0,0.01,2.0), aimag.linlin(0.00,1.0,0.01,10.0));

				IFFT(chain);

			},

			enhance: {|in, numPartials=0.5, ratio=0.25, strength=0.3|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				chain = PV_SpectralEnhance(chain, 
					numPartials.linlin(0.0,1.0, 1, 16),
					ratio.linlin(0.0,1.0,1.0,5.0), 
					strength.linlin(0.0,1.0,0.0,0.99)
				);

				IFFT(chain);
			},

			comb: {|in, teeth=0.5, comb=0.5|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				IFFT(PV_RectComb(chain, numTeeth: teeth*comb, width:1-comb)); 

			},

			binshift: {|in, binshift=0.5|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				IFFT(PV_BinShift(chain, stretch: binshift.range(0.01,4), shift: binshift.range(0,10), interp:0)); 

			},

			hbrick: {|in, hbrick=0.9|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				IFFT(PV_BrickWall(chain, wipe:hbrick)); 

			},

			lbrick: {|in, lbrick=0.0|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				IFFT(PV_BrickWall(chain, wipe:lbrick.linlin(0.0,1.0,0.0,-1.0))); 

			},

			diffuser: {|in, diffuse=0.2|
				var chain = Array.fill(numchans, {|i| FFT(LocalBuf(2048), in[i])});	

				IFFT(PV_Diffuser(chain, Dust.kr(diffuse.linlin(0.0,1.0,0.0,10.0)))); 

			},
		)
	)
}
