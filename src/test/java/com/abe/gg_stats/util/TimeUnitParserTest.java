package com.abe.gg_stats.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TimeUnitParserTest {

	@ParameterizedTest
	@CsvSource({ "30m, 30", "2h, 120", "1d, 1440", "1w, 10080", "3mo, 129600", "1y, 525600" })
	void testValidTimeFormats(String timeString, long expectedMinutes) throws ParseException {
		Duration duration = TimeUnitParser.parse(timeString);
		assertNotNull(duration);
		assertEquals(expectedMinutes, duration.toMinutes());
	}

	@Test
	void testMinutes() throws ParseException {
		Duration duration = TimeUnitParser.parse("45m");
		assertNotNull(duration);
		assertEquals(45, duration.toMinutes());
	}

	@Test
	void testHours() throws ParseException {
		Duration duration = TimeUnitParser.parse("12h");
		assertNotNull(duration);
		assertEquals(12, duration.toHours());
		assertEquals(720, duration.toMinutes());
	}

	@Test
	void testDays() throws ParseException {
		Duration duration = TimeUnitParser.parse("7d");
		assertNotNull(duration);
		assertEquals(7, duration.toDays());
		assertEquals(168, duration.toHours());
	}

	@Test
	void testWeeks() throws ParseException {
		Duration duration = TimeUnitParser.parse("2w");
		assertNotNull(duration);
		assertEquals(14, duration.toDays());
		assertEquals(336, duration.toHours());
	}

	@Test
	void testMonths() throws ParseException {
		Duration duration = TimeUnitParser.parse("6mo");
		assertNotNull(duration);
		assertEquals(180, duration.toDays()); // 6 * 30 days
	}

	@Test
	void testYears() throws ParseException {
		Duration duration = TimeUnitParser.parse("2y");
		assertNotNull(duration);
		assertEquals(730, duration.toDays()); // 2 * 365 days
	}

	@Test
	void testLargeValues() throws ParseException {
		Duration duration = TimeUnitParser.parse("1000h");
		assertNotNull(duration);
		assertEquals(1000, duration.toHours());
		assertEquals(60000, duration.toMinutes());
	}

	@Test
	void testZeroValues() throws ParseException {
		Duration duration = TimeUnitParser.parse("0h");
		assertNotNull(duration);
		assertEquals(0, duration.toHours());
	}

	@ParameterizedTest
	@ValueSource(strings = { "invalid", "1", "1x", "1.5h", "-1h", "1 hour", "1h30m", "1d2h", "1mo2w" })
	void testInvalidFormats(String invalidTime) {
		assertThrows(ParseException.class, () -> TimeUnitParser.parse(invalidTime));
	}

	@Test
	void testNullInput() {
		assertThrows(ParseException.class, () -> TimeUnitParser.parse(null));
	}

	@Test
	void testEmptyInput() {
		assertThrows(ParseException.class, () -> TimeUnitParser.parse(""));
	}

	@Test
	void testWhitespaceInput() {
		assertThrows(ParseException.class, () -> TimeUnitParser.parse("   "));
	}

	@Test
	void testCaseInsensitive() throws ParseException {
		Duration duration1 = TimeUnitParser.parse("2H");
		Duration duration2 = TimeUnitParser.parse("2h");

		assertNotNull(duration1);
		assertNotNull(duration2);
		assertEquals(duration1, duration2);
	}

	@Test
	void testWhitespaceAroundInput() throws ParseException {
		Duration duration = TimeUnitParser.parse("  2h  ");
		assertNotNull(duration);
		assertEquals(2, duration.toHours());
	}

	@Test
	void testParseToMonthsWithInvalidInput() {
		assertThrows(ParseException.class, () -> TimeUnitParser.parse("invalid"));
	}

	@Test
	void testEdgeCases() throws ParseException {
		// Very large values
		Duration duration = TimeUnitParser.parse("999999d");
		assertNotNull(duration);
		assertEquals(999999, duration.toDays());

		// Single character units
		Duration minutes = TimeUnitParser.parse("1m");
		assertNotNull(minutes);
		assertEquals(1, minutes.toMinutes());
	}

}
