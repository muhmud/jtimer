package timer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.lib.Format;
import timer.lib.Formatted;

@AllArgsConstructor
public class TimerTaskReport implements Formatted {
	private static final int MAX_TASK_LENGTH = 90;
	private static final String LINE_FORMAT = "%s\t\u001B[32m%s\u001B[0m";

	@Getter
	private final String task;

	@Getter
	Long timeSpent;

	@Override
	public String format() {
		final StringBuilder builder = new StringBuilder();

		builder.append(
				String.format(LINE_FORMAT, Format.pad(Format.formatName(task, MAX_TASK_LENGTH), MAX_TASK_LENGTH),
						Format.formatInterval(timeSpent)));

		return builder.toString();
	}
}
