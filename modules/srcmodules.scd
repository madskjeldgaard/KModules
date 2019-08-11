// TODO: Add modules
{|numchans|
	(
		osc: (
			sine: {|freq|
				SinOsc.ar(freq.linexp(0.0,1.0,1.0,20000.0))!numchans
			}
		)
	)
}
