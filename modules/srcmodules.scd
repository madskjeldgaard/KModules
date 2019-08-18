// TODO: Add modules
{|numchans|
(
	osc: (
		sine: {|freq|
			SinOsc.ar(freq.linexp(0.0,1.0,20.0,20000.0))!numchans },
		tri: {|freq|
			DPW3Tri.ar(freq.linexp(0.0,1.0,20.0,20000.0))!numchans
		},
		lftri: {|freq|
			LFTri.ar(freq.linexp(0.0,1.0,20.0,12000.0), iphase: rrand(0.0, 2pi))!numchans
		}
	),
)
}
