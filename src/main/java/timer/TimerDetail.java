package timer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.lib.Format;

@AllArgsConstructor
public class TimerDetail {
	private static final int MAX_TASK_LENGTH = 64;
	private static final String TASK_FORMAT = "\u001B[33m%s\u001B[0m";
	private static final String DATE_FORMAT = "E dd MMM yyyy";
	private static final String LINE_FORMAT = "    %s            %s";
	private static final String TOTAL = Format.pad("", 15);

	@Getter
	private final String task;

	@Getter
	private final SortedMap<Date, Long> timeSpent;

	public String format() {
		final StringBuilder builder = new StringBuilder();
		builder.append(String.format(TASK_FORMAT, Format.formatName(task, MAX_TASK_LENGTH)));
		builder.append("\n");

		final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		long total = 0l;
		for (Map.Entry<Date, Long> entry : timeSpent.entrySet()) {
			builder.append(String.format(LINE_FORMAT, dateFormat.format(entry.getKey()),
					Format.formatInterval(entry.getValue())));
			builder.append("\n");

			total += entry.getValue();
		}

		builder.append(
				String.format(LINE_FORMAT, TOTAL, "\u001B[32m" + Format.formatInterval(total) + "\u001B[0m"));

		return builder.toString();
	}
}
