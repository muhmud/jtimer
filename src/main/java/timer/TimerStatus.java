package timer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.exception.BadStatusFileException;
import timer.lib.Format;
import timer.lib.Formatted;

@AllArgsConstructor
public final class TimerStatus implements Formatted {
	private static final String LINE_FORMAT = "%s,%s\n";
	private static final String STATUS_FORMAT = "%s - %s";
	private static final String NULL = "null";

	@Getter
	private final String project;

	@Getter
	private final Date start;

	@Override
	public String format() {
		return String.format(STATUS_FORMAT, project,
				Format.formatInterval((new Date().getTime() - start.getTime()) / 1000));
	}

	public static TimerStatus parse(String statusLine) throws BadStatusFileException {
		if (statusLine != null && !statusLine.toLowerCase().equals("null") && statusLine.trim().length() > 0) {
			final String[] parts = statusLine.split(",");
			if (parts.length != 2) {
				throw new BadStatusFileException();
			}

			final String project = parts[0].trim();

			final Date start;
			try {
				start = new Date(Long.valueOf(parts[1].trim()));
			} catch (Exception e) {
				throw new BadStatusFileException();
			}

			return new TimerStatus(project, start);
		}

		return null;
	}

	public static void write(FileWriter writer, String project, Date start) throws IOException {
		if (start != null) {
			writer.write(String.format(LINE_FORMAT, project, start.getTime()));
		} else {
			writer.write(NULL);
		}
	}
}
