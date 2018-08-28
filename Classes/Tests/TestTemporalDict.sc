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
		this.assertEquals(td.rewind_data_seconds(0.5)[key], 123, "step 1");
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
}

TemporalDictTester {
	*new {
		^super.new.init();
	}

	init {
		TemporalDictMethodTester.run;
	}
}