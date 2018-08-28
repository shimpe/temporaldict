TemporalDict {

	var <>starttime;
	var <>mostrecentupdate;
	var <>eventlist;
	var <>datadict;
	var <>snapshot_dict;
	var <>snapshot_time;

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
			if ((this.mostrecentupdate - evlist['t']) < seconds) {
				var operation = evlist['replace'];
				reconstructedDict[operation['key']] = operation['old'];
			};
		});
		^reconstructedDict;
	}

	reset_forward_rewind {
		this.snapshot_dict = nil;
		this.snapshot_time = 0;
	}

	pr_sorted { | a, b, c|
		^( (a < b) && (b <= c) );
	}

	forward_data_seconds { | seconds = 0 |
		if (seconds < 0) {
			^this.rewind_data(seconds.neg);
		} {
			if (this.snapshot_dict.isNil) {
				this.snapshot_dict = this.get_data_seconds_from_begin(seconds);
				this.snapshot_time = seconds;
				^this.snapshot_dict;
			} {
				var orig_snapshot_time = this.snapshot_time;
				var reconstructedDict = this.snapshot_dict;
				this.eventlist.do({ | ev |
					if (this.pr_sorted(this.snapshot_time, ev['t'], orig_snapshot_time + seconds)) {
						var operation = ev['replace'];
						reconstructedDict[operation['key']] = operation['new'];
						this.snapshot_time = ev['t'];
					}
				});
				this.snapshot_time = orig_snapshot_time + seconds;
				^reconstructedDict;
			}
		}
		^nil; // should never be reached
	}

	rewind_data_seconds { | seconds = 0 |
		if (seconds < 0) {
			^this.forward_data(seconds.neg);
		} {
			if (this.snapshot_dict.isNil) {
				this.snapshot_dict = this.get_data_seconds_from_end(seconds);
				this.snapshot_time = this.mostrecentupdate - seconds;
				^this.snapshot_dict;
			} {
				var orig_snapshot_time = this.snapshot_time;
				var reconstructedDict = this.snapshot_dict;
				this.eventlist.reverseDo({ | ev |
					if (this.pr_sorted(orig_snapshot_time - seconds, ev['t'], this.snapshot_time)) {
						var operation = ev['replace'];
						reconstructedDict[operation['key']] = operation['old'];
						this.snapshot_time = ev['t'];
					}
				});
				this.snapshot_time = orig_snapshot_time - seconds;
				^reconstructedDict;
			}
		}
		^nil; // should never be reached
	}
}