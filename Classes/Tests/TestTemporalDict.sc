TemporalDictMethodTester : UnitTest {

    test_get_data_seconds_from_begin_single_key {
        var c = Condition.new();
        fork {
            var key = 10;
            var td = TemporalDict();
            0.1.wait;
            td.set(key, 123);
            0.2.wait;
            td.set(key, 100);
            0.2.wait;
            td.set(key, 0);
            0.2.wait;
            this.assertEquals(td.get_data_seconds_from_begin(0.11)[key], 123);
            this.assertEquals(td.get_data_seconds_from_begin(0.4)[key], 100);
            c.unhang;
        };
        c.wait;
    }

    test_get_data_seconds_from_begin_multi_key {
        var c = Condition.new();
        fork {
            var key1 = 10;
            var key2 = 12;
            var data;
            var td = TemporalDict();
            td.set(key1, 123);
            0.2.wait;
            td.set(key2, 987);
            0.1.wait;
            td.set(key1, 100);
            0.2.wait;
            td.set(key1, 0);
            td.set(key2, 75);
            0.2.wait;
            data = td.get_data_seconds_from_begin(0.4);
            this.assertEquals(data[key1], 100);
            this.assertEquals(data[key2], 987);
            c.unhang;
        };
        c.wait;
    }

    test_get_data_seconds_from_end_single_key {
        var c = Condition.new();
        fork {
            var key = 10;
            var td = TemporalDict();
            td.set(key, 123);
            0.2.wait;
            td.set(key, 100);
            0.2.wait;
            td.set(key, 0);
            this.assertEquals(td.get_data_seconds_from_end(0.3)[key], 123);
            c.unhang;
        };
        c.wait;
    }

    test_get_data_seconds_from_end_multi_key {
        var c = Condition.new();
        fork {
            var key1 = 10;
            var key2 = 12;
            var data;
            var td = TemporalDict();
            td.set(key1, 123);
            0.2.wait;
            td.set(key2, 987);
            0.1.wait;
            td.set(key1, 100);
            0.2.wait;
            td.set(key1, 0);
            td.set(key2, 75);
            0.2.wait;
            data = td.get_data_seconds_from_end(0.25);
            this.assertEquals(data[key1], 123);
            this.assertEquals(data[key2], 987);
            c.unhang;
        };
        c.wait;
    }

    test_forward {
        var c = Condition.new();
        fork {
            var key = 23;
            var td = TemporalDict();
            td.set(key, 765);
            0.1.wait;
            td.set(key, 345);
            0.1.wait;
            td.set(key, 123);

            this.assertEquals(td.forward_data_seconds(0.0)[key], nil, "step 0");
            this.assertEquals(td.forward_data_seconds(0.05)[key], 765, "step 1");
            this.assertEquals(td.forward_data_seconds(0.1)[key], 345, "step 2");
            this.assertEquals(td.forward_data_seconds(10.1)[key], 123, "step 3");
            c.unhang;
        };
        c.wait;
    }

    test_rewind {
        var c = Condition.new();
        fork {
            var key = 23;
            var td = TemporalDict();
            td.set(key, 765);
            0.1.wait;
            td.set(key, 345);
            0.1.wait;
            td.set(key, 123);
            this.assertEquals(td.rewind_data_seconds(0.0)[key], 123, "step 0");
            this.assertEquals(td.rewind_data_seconds(0.09)[key], 345, "step 1");
            this.assertEquals(td.rewind_data_seconds(0.09)[key], 765, "step 2");
            this.assertEquals(td.rewind_data_seconds(10.11)[key], nil, "step 3");
            c.unhang;
        };
        c.wait;
    }

    test_forward_and_rewind {
        var c = Condition.new();
        fork {
            var key = 23;
            var td = TemporalDict();
            td.set(key, 765);
            0.1.wait;
            td.set(key, 345);
            0.1.wait;
            td.set(key, 123);
            this.assertEquals(td.rewind_data_seconds(0.0)[key], 123, "step 0");
            this.assertEquals(td.rewind_data_seconds(0.09)[key], 345, "step 1");
            this.assertEquals(td.forward_data_seconds(0.09)[key], 123, "step 2");
            this.assertEquals(td.rewind_data_seconds(0.09)[key], 345, "step 1");
            this.assertEquals(td.rewind_data_seconds(0.09)[key], 765, "step 3");
            this.assertEquals(td.rewind_data_seconds(10.11)[key], nil, "step 4");
            c.unhang;
        };
        c.wait;
    }

    test_corner_case_fw_rew_I {
        var key = 23;
        var td = TemporalDict();
        td.set(key,123,0);
        td.set(key,180,1);
        td.set(key,100,2);
        this.assertEquals(td.forward_data_seconds(0.5)[key], 123, "step 0");
        this.assertEquals(td.rewind_data_seconds(0.5)[key], nil, "step 1"); // decisions, decisions
        this.assertEquals(td.forward_data_seconds(0.5)[key], 123, "step 2");
        this.assertEquals(td.rewind_data_seconds(0.49)[key], 123, "step 3");
    }

    test_corner_case_fw_rew_II {
        var key = 23;
        var td = TemporalDict();
        td.set(key,123,0);
        td.set(key,180,1);
        td.set(key,100,2);
        this.assertEquals(td.forward_data_seconds(0.4)[key], 123, "step 0");
        this.assertEquals(td.forward_data_seconds(0.1)[key], 123, "step 1");
        this.assertEquals(td.rewind_data_seconds(0.50001)[key], nil, "step 2");
        this.assertEquals(td.forward_data_seconds(0.00001)[key], 123, "step 3");
        this.assertEquals(td.forward_data_seconds(1)[key], 180, "step 4");
        this.assertEquals(td.rewind_data_seconds(0.00001)[key], 123, "step 5");
        this.assertEquals(td.forward_data_seconds(0.00001)[key], 180, "step 6");
        this.assertEquals(td.forward_data_seconds(100)[key], 100, "step 7");
        this.assertEquals(td.rewind_data_seconds(1000)[key], nil, "step 8");
    }

    test_initial_state {
        var key = 32;
        var td = TemporalDict.new(Dictionary.with(*[10->123, 32->456, 54->789]));
        td.set(key, 101112, 1);
        this.assertEquals(td.forward_data_seconds(0)[key], 456, "step 0");
        this.assertEquals(td.forward_data_seconds(2)[key], 101112, "step 1");
        this.assertEquals(td.rewind_data_seconds(5)[key], 456, "step 2");
    }

    test_time_to_index {
        var td = TemporalDict();
        var key = 5;
        td.set(key, 10, 1);
        td.set(key, 20, 2.4);
        td.set(key, 30, 3.9);
        td.set(key, 40, 4);
        td.set(key, 50, 5);
        this.assertEquals(td.pr_ceil(td.pr_time_to_index(2.5)), 2);
        this.assertEquals(td.pr_floor(td.pr_time_to_index(2.5), 4), 1);
        this.assertEquals(td.pr_floor(td.pr_time_to_index(10), 4), 4);
        this.assertEquals(td.pr_ceil(td.pr_time_to_index(10)), 4);
        this.assertEquals(td.pr_floor(td.pr_time_to_index(0.1),4), nil);
        this.assertEquals(td.pr_ceil(td.pr_time_to_index(0.1)), 0);
        this.assertEquals(td.pr_ceil(td.pr_time_to_index(3.9)), 2);
        this.assertEquals(td.pr_floor(td.pr_time_to_index(3.9),4), 2);
    }

    test_forward_after_get_data {
        var td = TemporalDict();
        td.set(5, 12, 1);
        td.set(5, 16, 2);
        td.set(5, 34, 3);
        td.set(5, 67, 4);
        this.assertEquals(td.get_data_seconds_from_begin(1.5)[5], 12);
        this.assertEquals(td.forward_data_seconds(1.1)[5], 16);
        td.reset_forward_rewind;
        this.assertEquals(td.forward_data_seconds(1.1)[5], 12);
    }

    test_performance {
        var td = TemporalDict();
        var tstart, tend;
        var profilingresults = [];
        var iterations = 100000;
        tstart = Date.localtime.rawSeconds;
        iterations.do({
            |i|
            td.set([1,2,3,4,5].choose, 0.rrand(127), i*10/iterations);
        });
        profilingresults = profilingresults.add(('avg set time' : (Date.localtime.rawSeconds - tstart)/iterations));
        tstart = Date.localtime.rawSeconds;
        iterations.do({
            var idx = td.pr_time_to_index(0.0.rrand(10.0));
        });
        profilingresults = profilingresults.add(('avg idx lookup time' : (Date.localtime.rawSeconds - tstart)/iterations));
        profilingresults.debug("profiling results");
    }
}

TemporalDictTester {
    *new {
        ^super.new.init();
    }

    init {
        //TemporalDictMethodTester.runTest("TemporalDictMethodTester:test_forward");
        TemporalDictMethodTester.run;
    }
}