TemporalDict {

	var <>starttime;
	var <>mostrecentupdate;
	var <>eventlist;
	var <>datadict;
	var <>iterator_dict;
	var <>iterator_time;

	*new {
		^super.new.init();
	}

	init {
		this.starttime = Date.localtime.rawSeconds;
		this.eventlist = [];
		this.datadict = Dictionary();
		this.mostrecentupdate = this.starttime;
		^this;
	}

	set { | key, value |
		var time = (Date.localtime.rawSeconds - this.starttime);
		this.eventlist = this.eventlist.add(('t' : time, 'replace' : ('key' : key, 'old' : datadict[key], 'new' : value)));
		this.datadict[key] = value;
		this.mostrecentupdate = time;
		^this;
	}

	remove { | key |
		this.set(key, nil);
		^this;
	}

	get_data_seconds_from_begin { | seconds = 0 |
		var reconstructedDict = Dictionary();
		this.eventlist.do({ | evlist |
			if (evlist['t'] <= seconds) {
				var operation = evlist['replace'];
				reconstructedDict[operation['key']] = operation['new'];
			};
		});
		^reconstructedDict;
	}

	get_data_seconds_from_end { | seconds = 0 |
		// shorter but possibly more expensive:
		/*var rel_time = this.mostrecentupdate - seconds;
		^this.get_data_seconds_from_begin(rel_time);*/

		var reconstructedDict = this.datadict;
		this.eventlist.reverseDo({
			| evlist |
			if ((this.mostrecentupdate - evlist['t']) <= seconds) {
				var operation = evlist['replace'];
				reconstructedDict[operation['key']] = operation['old'];
			};
		});
		^reconstructedDict;
	}

	reset_forward_rewind {
		this.iterator_dict = nil;
		this.iterator_time = 0;
	}

	forward_data { | seconds = 0 |
		if (seconds <= 0) {
			^this.rewind_data(seconds.neg);
		} {
			if (this.iterator_dict.isNil) {
				this.iterator_dict = this.get_data_seconds_from_begin(seconds);
				this.iterator_time = seconds;
				^this.iterator_dict;
			} {
				var reconstructedDict = this.iterator_dict;
				this.eventlist.do({ | evlist |
					if ((evlist['t'] > this.iterator_time) && (this.iterator_time <= (evlist['t'] + seconds))) {
						var operation = evlist['replace'];
						reconstructedDict[operation['key']] = operation['new'];
					};
				});
				this.iterator_time = this.iterator_time + seconds;
				^reconstructedDict;
			}
		}
		^nil; // should never be reached
	}

	rewind_data { | seconds = 0 |
		if (this.seconds <= 0) {
			^this.forward_data(seconds.neg);
		} {
			if (this.iterator_dict.isNil) {
				this.iterator_dict = this.get_data_seconds_from_end(seconds);
				this.iterator_time = this.mostrecentupdate - seconds;
				^this.iterator_dict;
			} {
				var reconstructedDict = this.iterator_dict;
				this.eventlist.reverseDo({ | evlist |
					if ((evlist['t'] > this.iterator_time) && (this.iterator_time <= (evlist['t'] + seconds))) {
						var operation = evlist['replace'];
						reconstructedDict[operation['new']] = operation['key'];
					};
				});
				this.iterator_time = this.iterator_time - seconds;
				^reconstructedDict;
			}
		}
		^nil; // should never be reached
	}
}