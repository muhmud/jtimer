package timer;

import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.exception.BadLogFileException;

@AllArgsConstructor
public final class TimerLog {
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS Z";
	private static final String LINE_FORMAT = "%s,%s,%s\n";
	private static final String NULL = "null";

	private static Date parseDate(SimpleDateFormat dateFormat, String date) throws ParseException {
		if (date == null || date.toLowerCase().equals(NULL) || date.trim().length() == 0) {
			return null;
		}

		return dateFormat.parse(date);
	}

	@Getter
	private final Date start;

	@Getter
	private final Date end;

	@Getter
	private final String task;

	public static TimerLog parse(String logLine) throws BadLogFileException {
		if (logLine != null && logLine.trim().length() > 0) {
			final String[] parts = logLine.split(",");
			if (parts.length != 3) {
				throw new BadLogFileException();
			}

			final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			final Date start, end;
			try {
				start = parseDate(dateFormat, parts[0].trim());
				end = parseDate(dateFormat, parts[1].trim());
			} catch (ParseException e) {
				throw new BadLogFileException();
			}

			final String task = parts[2].trim();

			return new TimerLog(start, end, task);
		}

		return null;
	}

	public static void write(FileWriter writer, TimerLog timerLog) throws IOException {
		final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		writer.write(String.format(LINE_FORMAT, dateFormat.format(timerLog.getStart()),
				timerLog.getEnd() != null ? dateFormat.format(timerLog.getEnd()) : null, timerLog.getTask()));
	}
}
