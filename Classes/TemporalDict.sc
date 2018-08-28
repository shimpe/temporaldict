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

	set { | key, value, seconds_since_start=nil |
		var time;
		if (seconds_since_start.isNil) {
			time = (Date.localtime.rawSeconds - this.starttime);
		} {
			time = seconds_since_start;
		};
		if ((this.starttime + time) <= this.mostrecentupdate) {
			"Error. Time cannot run backwards.".error;
		};
		this.eventlist = this.eventlist.add(('t' : time, 'replace' : ('key' : key, 'old' : datadict[key], 'new' : value)));
		this.datadict[key] = value;
		this.mostrecentupdate = time;
		^this;
	}

	remove { | key, seconds_since_start=nil |
		this.set(key, nil, seconds_since_start);
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
			// negative forwarding is rewinding
			^this.rewind_data(seconds.neg);
		} {
			if (this.snapshot_dict.isNil) {
				// if no iterations happened before, initialize state from scratch
				this.snapshot_dict = this.get_data_seconds_from_begin(seconds);
				this.snapshot_time = seconds;
				^this.snapshot_dict;
			} {
				// if iterations happened before, build on the previous state to calculate the new state
				var orig_snapshot_time = this.snapshot_time;
				var reconstructedDict = this.snapshot_dict;
				this.eventlist.do({ | ev |
					// if event happened between previous snapshot and end time...
					if (this.pr_sorted(this.snapshot_time, ev['t'], orig_snapshot_time + seconds)) {
						// apply reconstruction
						var operation = ev['replace'];
						reconstructedDict[operation['key']] = operation['new'];
						this.snapshot_time = ev['t'];
					}
				});
				// update snapshot time to new snapshot time
				this.snapshot_time = orig_snapshot_time + seconds;
				^reconstructedDict;
			}
		}
		^nil; // should never be reached
	}

	rewind_data_seconds { | seconds = 0 |
		if (seconds < 0) {
			// negative rewinding is forwarding
			^this.forward_data(seconds.neg);
		} {
			// if not iterations happened before, initialize data from scratch
			if (this.snapshot_dict.isNil) {
				this.snapshot_dict = this.get_data_seconds_from_end(seconds);
				this.snapshot_time = this.mostrecentupdate - seconds;
				^this.snapshot_dict;
			} {
				// if iterations happened before, build on previous state to calculate new state
				var orig_snapshot_time = this.snapshot_time;
				var reconstructedDict = this.snapshot_dict;
				// running backwards through events
				this.eventlist.reverseDo({ | ev |
					// if event happened before previous snapshot but after end time...
					if (this.pr_sorted(orig_snapshot_time - seconds, ev['t'], this.snapshot_time)) {
						// restore old value
						var operation = ev['replace'];
						reconstructedDict[operation['key']] = operation['old'];
						this.snapshot_time = ev['t'];
					}
				});
				// update snapshot time to new snapshot time
				this.snapshot_time = orig_snapshot_time - seconds;
				^reconstructedDict;
			}
		}
		^nil; // should never be reached
	}
}