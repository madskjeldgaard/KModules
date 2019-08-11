{|numchans|
	(
		osc: (
			sine: {|freq|
				SinOsc.ar(freq)!numchans
			}
		)
	)
}
