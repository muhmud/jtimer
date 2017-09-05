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
	private static final String LINE_FORMAT = "%s,%s,%s,%s,%s,%s,%s\n";
	private static final String STATUS_FORMAT = "%s - %s";
	private static final String NULL = "null";

	public enum Status {
		RUNNING, PAUSED, STOPPED
	}

	@Getter
	private final Status status;

	@Getter
	private final String project;

	@Getter
	private final String directory;

	@Getter
	private final Date anchor;

	@Getter
	private final Long workDone;

	@Getter
	private final Date start;

	@Getter
	private final Date end;

	@Override
	public String format() {
		return String.format(STATUS_FORMAT, project,
				Format.formatInterval((new Date().getTime() - start.getTime()) / 1000 + workDone));
	}

	public static TimerStatus parse(String statusLine) throws BadStatusFileException {
		if (statusLine != null && !statusLine.toLowerCase().equals("null") && statusLine.trim().length() > 0) {
			final String[] parts = statusLine.split(",");
			if (parts.length != 7) {
				throw new BadStatusFileException();
			}

			int part = 0;
			final String status = parts[part++].trim();
			final String project = parts[part++].trim();
			final String directory = parts[part++].trim();

			final Date anchor;
			final Long workDone;
			final Date start;
			final Date end;
			try {
				anchor = new Date(Long.valueOf(parts[part++].trim()));
				workDone = Long.valueOf(parts[part++].trim());
				start = new Date(Long.valueOf(parts[part++].trim()));

				final String endString = parts[part++].trim();
				if (!endString.equals("null")) {
					end = new Date(Long.valueOf(endString));
				} else {
					end = null;
				}
			} catch (Exception e) {
				throw new BadStatusFileException();
			}

			return new TimerStatus(Status.valueOf(status), project, directory, anchor, workDone, start, end);
		}

		return null;
	}

	public static void write(FileWriter writer, Status status, String project, String directory, Date anchor,
			Long workDone, Date start, Date end) throws IOException {
		if (start != null) {
			writer.write(String.format(LINE_FORMAT, status.name(), project, directory, anchor.getTime(), workDone,
					start.getTime(), end == null ? "null" : end.getTime()));
		} else {
			writer.write(NULL);
		}
	}
}
