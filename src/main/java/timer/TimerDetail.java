package timer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.lib.Format;
import timer.lib.Formatted;

@AllArgsConstructor
public class TimerDetail implements Formatted {
	private static final int MAX_TASK_LENGTH = 64;
	private static final String DATE_FORMAT = "E dd MMM yyyy";
	private static final String LINE_FORMAT = "    %s   %s";
	private static final String TOTAL = Format.pad("", MAX_TASK_LENGTH);

	@Getter
	private final Date date;

	@Getter
	private final SortedMap<String, Long> timeSpent;

	@Override
	public String format() {
		final StringBuilder builder = new StringBuilder();

		final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		builder.append(dateFormat.format(date));
		builder.append("\n");

		long total = 0l;
		for (Map.Entry<String, Long> entry : timeSpent.entrySet()) {
			builder.append(String.format(LINE_FORMAT,
					Format.pad(Format.formatName(entry.getKey(), MAX_TASK_LENGTH), MAX_TASK_LENGTH),
					Format.formatInterval(entry.getValue())));
			builder.append("\n");

			total += entry.getValue();
		}

		builder.append(
				String.format(LINE_FORMAT, TOTAL, "\u001B[32m" + Format.formatInterval(total) + "\u001B[0m"));

		return builder.toString();
	}
}
