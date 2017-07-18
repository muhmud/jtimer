package timer;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.lib.Formatted;

@AllArgsConstructor
public class TimerSummary implements Formatted {
	private static final String DATE_FORMAT = "E dd MMM yyyy";
	private static final String LINE_FORMAT = "%s: %s";

	@Getter
	private final Date date;

	@Getter
	private final BigDecimal time;

	public String format() {
		final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		return String.format(LINE_FORMAT, dateFormat.format(date), time.toString());
	}
}
