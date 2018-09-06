TemporalDict {
    var <>initialstate;
    var <>starttime;
    var <>mostrecentupdate;
    var <>eventlist;
    var <>datadict;
    var <>snapshot_dict;
    var <>snapshot_time;

    *new { | initialstate=nil |
        ^super.new.init(initialstate);
    }

    init { | initialstate |
        if (initialstate.isNil) {
            this.initialstate = Dictionary();
        } {
            this.initialstate = initialstate.copy();
        };
        this.starttime = Date.localtime.rawSeconds;
        this.eventlist = [];
        this.datadict = this.initialstate.copy();
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
        if ((this.starttime + time) < this.mostrecentupdate) {
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
        var reconstructedDict = this.initialstate.copy();
        this.eventlist.do({ | evlist |
            if (evlist['t'] <= seconds) {
                var operation = evlist['replace'];
                reconstructedDict[operation['key']] = operation['new'];
            };
        });

        // update data used by rewind/forward operations
        this.snapshot_dict = reconstructedDict.copy();
        this.snapshot_time = seconds;

        ^reconstructedDict;
    }

    get_data_seconds_from_end { | seconds = 0 |
        // shorter but possibly more expensive:
        /*var rel_time = this.mostrecentupdate - seconds;
        ^this.get_data_seconds_from_begin(rel_time);*/
        var reconstructedDict = this.datadict.copy();
        this.eventlist.reverseDo({
            | evlist |
            if ((this.mostrecentupdate - evlist['t']) < seconds) {
                var operation = evlist['replace'];
                reconstructedDict[operation['key']] = operation['old'];
            };
        });

        // update data used by rewind/forward operations
        this.snapshot_dict = reconstructedDict.copy();
        this.snapshot_time = seconds;

        ^reconstructedDict;
    }

    reset_forward_rewind {
        this.snapshot_dict = nil;
        this.snapshot_time = 0;
    }

    pr_float_equal { | f1, f2, tol=1e-6 |
        ^((f1 - f2).abs <= (tol * (f1.abs.max(f2.abs))));
    }

    pr_time_to_index { | time |
        var lowerbound = 0;
        var upperbound = this.eventlist.size-1;
        if (this.eventlist.size == 0) {
            ^nil;
        };
        if (this.eventlist.size == 1) {
            ^0;
        };
        if (time > this.eventlist[upperbound]['t']) {
            ^upperbound;
        };
        if (time < this.eventlist[0]['t']) {
            ^nil;
        };
        while ({(upperbound-lowerbound) > 1}, {
            if (this.pr_float_equal(time, this.eventlist[lowerbound]['t'])) {
                ^lowerbound;
            } {
                if (this.pr_float_equal(time, this.eventlist[upperbound]['t'])) {
                    ^upperbound;
                } {
                    if (time > this.eventlist[lowerbound]['t']) {
                        var newbound = ((lowerbound + upperbound)/2).asInt;
                        if (time < this.eventlist[newbound]['t']) {
                            upperbound = newbound;
                        } {
                            if (time > this.eventlist[newbound]['t']) {
                                lowerbound = newbound;
                            } {
                                ^newbound;
                            };
                        };
                    };
                };
            };
        });
        if (upperbound == lowerbound) {
            ^lowerbound;
        } {
            var fractionalpart= time.linlin(this.eventlist[lowerbound]['t'], this.eventlist[upperbound]['t'], 0, 1);
            ^(lowerbound + fractionalpart);
        };
    }

    pr_floor { | value, maxidx |
        if (value.isNil) {^value};
        if (value == inf) { ^maxidx };
        ^value.floor;
    }

    pr_ceil { | value |
        if (value.isNil) { ^0; };
        if (value == inf) { ^value; };
        ^value.ceil;
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
                var first_relevant_event = this.pr_ceil(this.pr_time_to_index(orig_snapshot_time));
                var latest_relevant_event = this.pr_floor(this.pr_time_to_index(orig_snapshot_time + seconds), this.eventlist.size-1);
                if (first_relevant_event.isNil && latest_relevant_event.notNil) {
                    first_relevant_event = 0;
                } {
                    if (first_relevant_event.isNil) {
                        ^reconstructedDict;
                    };
                };
                if (first_relevant_event == inf) {
                    ^reconstructedDict;
                };
                if (latest_relevant_event.isNil) {
                    ^reconstructedDict;
                };
                if (latest_relevant_event == inf) {
                    latest_relevant_event = this.eventlist.size-1;
                };
                if (latest_relevant_event >= first_relevant_event) {
                    (first_relevant_event..latest_relevant_event).do({ |index|
                        var operation = this.eventlist[index]['replace'];
                        reconstructedDict[operation['key']] = operation['new'];
                    });
                };
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
                var latest_relevant_event = this.pr_floor(this.pr_time_to_index(orig_snapshot_time), this.eventlist.size-1);
                var first_relevant_event = this.pr_ceil(this.pr_time_to_index(orig_snapshot_time - seconds));
                if (first_relevant_event.isNil && latest_relevant_event.notNil) {
                    first_relevant_event = 0;
                } {
                    if (first_relevant_event.isNil) {
                        ^reconstructedDict;
                    };
                };
                if (first_relevant_event == inf) {
                    ^reconstructedDict;
                };
                if (latest_relevant_event.isNil) {
                    ^reconstructedDict;
                };
                if (latest_relevant_event == inf) {
                    latest_relevant_event = this.eventlist.size-1;
                };
                // running backwards through events
                if (first_relevant_event <= latest_relevant_event) {
                    (latest_relevant_event..first_relevant_event).do({ |index|
                        var operation = this.eventlist[index]['replace'];
                        reconstructedDict[operation['key']] = operation['old'];
                    });
                };
                // update snapshot time to new snapshot time
                this.snapshot_time = orig_snapshot_time - seconds;
                ^reconstructedDict;
            }
        }
        ^nil; // should never be reached
    }
}