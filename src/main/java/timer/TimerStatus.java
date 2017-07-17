package timer;

import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.exception.BadStatusFileException;

@AllArgsConstructor
public final class TimerStatus {
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS Z";
	private static final String LINE_FORMAT = "%s,%s\n";
	private static final String STATUS_FORMAT = "%s - %s:%s:%s";
	private static final String NULL = "null";

	private static final int SECONDS_MINUTE = 60;
	private static final int SECONDS_HOUR = SECONDS_MINUTE * 60;

	@Getter
	private final String project;

	@Getter
	private final Date start;

	private static String format(long number) {
		return String.format(number < 10 ? "0%d" : "%d", number);
	}

	public String format() {
		final Date now = new Date();
		long timeDifference = (now.getTime() - start.getTime()) / 1000;

		final long hours = (timeDifference - (timeDifference % SECONDS_HOUR)) / SECONDS_HOUR;
		timeDifference -= hours * SECONDS_HOUR;

		final long minutes = (timeDifference - (timeDifference % SECONDS_MINUTE)) / SECONDS_MINUTE;
		timeDifference -= minutes * SECONDS_MINUTE;

		final long seconds = timeDifference;

		return String.format(STATUS_FORMAT, project, format(hours), format(minutes), format(seconds));
	}

	public static TimerStatus parse(String statusLine) throws BadStatusFileException {
		if (statusLine != null && !statusLine.toLowerCase().equals("null") && statusLine.trim().length() > 0) {
			final String[] parts = statusLine.split(",");
			if (parts.length != 2) {
				throw new BadStatusFileException();
			}

			final String project = parts[0].trim();

			final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			final Date start;
			try {
				start = dateFormat.parse(parts[1].trim());
			} catch (ParseException e) {
				throw new BadStatusFileException();
			}

			return new TimerStatus(project, start);
		}

		return null;
	}

	public static void write(FileWriter writer, String project, Date start) throws IOException {
		if (start != null) {
			final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

			writer.write(String.format(LINE_FORMAT, project, dateFormat.format(start)));
		} else {
			writer.write(NULL);
		}
	}
}
