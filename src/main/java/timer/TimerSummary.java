package timer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.lib.Format;
import timer.lib.Formatted;

@AllArgsConstructor
public class TimerSummary implements Formatted {
	private static final String DATE_FORMAT = "E dd MMM yyyy (hh:mm)";
	private static final String LINE_FORMAT = "%s:   \u001B[32m%s\u001B[0m (%s)";

	@Getter
	private final Date date;

	@Getter
	private final long time;

	@Override
	public String format() {
		final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		final BigDecimal hours = BigDecimal.valueOf(time).divide(BigDecimal.valueOf(3600), 2, RoundingMode.DOWN);

		return String.format(LINE_FORMAT, dateFormat.format(date), Format.formatInterval(time), hours.toString());
	}
}
