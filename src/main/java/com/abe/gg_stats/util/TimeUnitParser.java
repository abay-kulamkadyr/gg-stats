package com.abe.gg_stats.util;

import java.text.ParseException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * Utility class to parse flexible time unit strings into Duration objects. Supports
 * various time units: minutes (m), hours (h), days (d), weeks (w), months (mo), years (y)
 * <p>
 * Examples: - "30m" = 30 minutes - "2h" = 2 hours - "1d" = 1 day - "1w" = 1 week - "3mo"
 * = 3 months - "1y" = 1 year
 */
public class TimeUnitParser {

	private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)(mo|[mhdw]|y)$");

	private TimeUnitParser() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

	/**
	 * Parse a time string into a Duration
	 * @param timeString time string like "2h", "30m", "1d", "1w", "3mo", "1y"
	 * @return Duration object, or throw ParseException
	 */
	public static Duration parse(String timeString) throws ParseException {
		if (!StringUtils.hasText(timeString)) {
			throw new ParseException("Time string cannot be empty or null.", 0);
		}

		Matcher matcher = TIME_PATTERN.matcher(timeString.trim().toLowerCase());
		if (!matcher.matches()) {
			throw new ParseException("Invalid time string format: '" + timeString + "'. "
					+ "The string must be a number followed by a valid unit (m, h, d, w, mo, y).", 0);
		}

		try {
			long value = Long.parseLong(matcher.group(1));
			String unit = matcher.group(2);

			return switch (unit) {
				case "m" -> Duration.ofMinutes(value);
				case "h" -> Duration.ofHours(value);
				case "d" -> Duration.ofDays(value);
				case "w" -> Duration.ofDays(value * 7);
				case "mo" -> Duration.ofDays(value * 30);
				case "y" -> Duration.ofDays(value * 365);
				default -> throw new IllegalStateException("Unexpected unit: " + unit);
			};
		}
		catch (NumberFormatException e) {
			throw new ParseException("Value is too large: '" + timeString + "'", 0);
		}
	}

}
