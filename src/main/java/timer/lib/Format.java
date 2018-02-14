package timer.lib;

import java.util.Collections;

public final class Format {
	private static final String INTERVAL_FORMAT = "%s:%s:%s";

	private static final int SECONDS_MINUTE = 60;
	private static final int SECONDS_HOUR = SECONDS_MINUTE * 60;

	private static String format(long number) {
		return String.format(number < 10 ? "0%d" : "%d", number);
	}

	private Format() {}

	public static String formatInterval(long timeDifferenceSeconds) {
		final long hours = (timeDifferenceSeconds - (timeDifferenceSeconds % SECONDS_HOUR)) / SECONDS_HOUR;
		timeDifferenceSeconds -= hours * SECONDS_HOUR;

		final long minutes = (timeDifferenceSeconds - (timeDifferenceSeconds % SECONDS_MINUTE)) / SECONDS_MINUTE;
		timeDifferenceSeconds -= minutes * SECONDS_MINUTE;

		final long seconds = timeDifferenceSeconds;

		return String.format(INTERVAL_FORMAT, format(hours), format(minutes), format(seconds));
	}

	public static String formatName(String name, int maxLength) {
		if (name != null) {
			if (name.length() > maxLength && maxLength > 4) {
				return name.substring(0, maxLength - 4) + " ...";
			}
		}

		return name;
	}

	public static String pad(String value, int to) {
		if (value != null) {
			if (value.length() < to) {
				return value + String.join("", Collections.nCopies(to - value.length(), " "));
			}
		}

		return value;
	}
}
