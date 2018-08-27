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
}


TemporalDictTester {
	*new {
		^super.new.init();
	}

	init {
		TemporalDictMethodTester.run;
	}
}